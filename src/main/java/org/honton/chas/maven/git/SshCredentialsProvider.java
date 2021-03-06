package org.honton.chas.maven.git;

import com.google.common.base.Function;
import org.apache.maven.plugin.logging.Log;
import org.apache.maven.settings.Server;
import org.eclipse.jgit.transport.CredentialItem;
import org.eclipse.jgit.transport.URIish;

/**
 * Find ssh credentials in settings.xml - privateKey/[passphrase] entries are supported
 */
class SshCredentialsProvider extends SettingsXmlCredentialsProvider {

  public SshCredentialsProvider(Log log, Function<String, Server> servers) {
    super(log, servers);
  }

  protected boolean isSupported(CredentialItem credentialItem) {
    return credentialItem instanceof CredentialItem.YesNoType || super.isSupported(credentialItem);
  }

  protected String checkItem(Server server, CredentialItem credentialItem) {
    if (credentialItem instanceof CredentialItem.YesNoType) {
      log.debug(credentialItem.getPromptText());
      if (credentialItem.getPromptText().contains("RSA key fingerprint")) {
        ((CredentialItem.YesNoType) credentialItem).setValue(true);
        log.debug("Returning 'yes' to fingerprint query");
        return null;
      }
      return "Cannot support credential with prompt " + credentialItem.getPromptText();
    }
    return super.checkItem(server, credentialItem);
  }

  protected String getId(URIish urIish) {
    int port = urIish.getPort();
    if (-1 != port && 22 != port) {
      return urIish.getHost() + ':' + port;
    } else {
      return urIish.getHost();
    }
  }
}
