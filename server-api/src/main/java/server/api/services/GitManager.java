package server.api.services;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import javax.inject.Singleton;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;

@Singleton
public class GitManager {

  private Git git;

  public String cloneOrPull(String uri, String name) throws IOException {

    Path repoPath = Paths.get("/tmp", name);

    try {
      if (Files.exists(repoPath)) {
        pull(uri, repoPath.toFile());
      } else {
        clone(uri, repoPath.toFile());
      }
    } catch (GitAPIException | IOException ex) {
      throw new IOException(ex);
    }
    return repoPath.toString();
  }

  private void clone(String uri, File dir) throws GitAPIException {
    git = Git.cloneRepository()
        .setURI(uri)
        .setDirectory(dir)
        .call();
  }

  private void pull(String uri, File dir) throws IOException {
    Git.open(dir)
        .pull();
  }
}
