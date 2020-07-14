package server.api.models;

import server.api.models.gjthub.Repository;

public class GithubHook {

  private String ref;
  private String before;
  private String after;

  private Repository repository;

  public String getRef() {
    return ref;
  }

  public void setRef(String ref) {
    this.ref = ref;
  }

  public String getBefore() {
    return before;
  }

  public void setBefore(String before) {
    this.before = before;
  }

  public String getAfter() {
    return after;
  }

  public void setAfter(String after) {
    this.after = after;
  }

  public Repository getRepository() {
    return repository;
  }

  public void setRepository(Repository repository) {
    this.repository = repository;
  }
}
