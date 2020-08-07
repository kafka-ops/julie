package server.api.models.gjthub;

public class Repository {

  private String name;
  private String full_name;
  private Owner owner;
  private String html_url;
  private String description;
  private String url;
  private String clone_url;

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public String getFull_name() {
    return full_name;
  }

  public void setFull_name(String full_name) {
    this.full_name = full_name;
  }

  public Owner getOwner() {
    return owner;
  }

  public void setOwner(Owner owner) {
    this.owner = owner;
  }

  public String getHtml_url() {
    return html_url;
  }

  public void setHtml_url(String html_url) {
    this.html_url = html_url;
  }

  public String getDescription() {
    return description;
  }

  public void setDescription(String description) {
    this.description = description;
  }

  public String getUrl() {
    return url;
  }

  public void setUrl(String url) {
    this.url = url;
  }

  public String getClone_url() {
    return clone_url;
  }

  public void setClone_url(String clone_url) {
    this.clone_url = clone_url;
  }
}
