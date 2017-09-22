package org.honton.chas.maven.git;

import com.google.common.base.Function;
import com.google.common.base.Predicate;
import com.google.common.collect.Iterables;
import java.io.File;
import java.io.IOException;
import java.util.List;
import lombok.EqualsAndHashCode;
import lombok.RequiredArgsConstructor;
import org.apache.maven.plugin.logging.Log;
import org.apache.maven.settings.Server;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.PushCommand;
import org.eclipse.jgit.api.TagCommand;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revwalk.RevObject;
import org.eclipse.jgit.revwalk.RevWalk;
import org.eclipse.jgit.storage.file.FileRepositoryBuilder;

@RequiredArgsConstructor
@EqualsAndHashCode(exclude = {"gitDir", "log", "servers"})
class TagGit {

  final private String branch;
  final private String remote;
  final private String tagName;
  final private String message;
  final private boolean skipPush;
  final private boolean useUseDotSsh;

  private File gitDir;
  private Log log;
  private List<Server> servers;
  private Function<String,Server> serverAccess;

  public String createKey(File baseDir) throws IOException {
    gitDir = new FileRepositoryBuilder().findGitDir(baseDir).getGitDir();
    return gitDir.getCanonicalPath();
  }

  public void tagAndPush(Log log, final List<Server> servers) throws IOException, GitAPIException {
    this.log = log;
    this.servers = servers;
    serverAccess = new Function<String, Server>() {
      @Override
      public Server apply(final String id) {
        return Iterables.find(servers, new Predicate<Server>() {
          @Override
          public boolean apply(Server server) {
            return server.getId().equals(id);
          }
        }, null);
      }
    };
    try (Repository repository = new FileRepositoryBuilder().setGitDir(gitDir).build()) {
      try (Git git = new Git(repository)) {
        tag(git);
        if (!skipPush) {
          push(git);
        }
      }
    }
  }

  private void tag(Git git) throws GitAPIException, IOException {
    log.debug("tagging branch:"+branch+" tag:"+tagName);
    TagCommand tagCommand = git.tag().setAnnotated(true);
    if (branch != null) {
      tagCommand.setObjectId(getObjectId(git));
    }
    tagCommand.setName(tagName).setMessage(message == null ? "release " + tagName : message).call();
  }

  private RevObject getObjectId(Git git) throws IOException {
    Repository repository = git.getRepository();
    ObjectId objectId = repository.findRef(branch).getObjectId();
    return new RevWalk(repository).parseAny(objectId);
  }

  private void push(Git git) throws GitAPIException {
    PushCommand pushCommand = git.push().setPushTags();
    if (remote != null) {
      pushCommand.setRemote(remote);
    }
    pushCommand.setCredentialsProvider(new SettingsXmlCredentialsProvider(log, serverAccess)).call();
    if (!useUseDotSsh) {
      pushCommand.setTransportConfigCallback(new SettingsXmlConfigCallback(log, servers));
    }
  }
}