package com.purbon.kafka.topology.api.mds.http;

import java.net.URI;
import org.apache.http.client.methods.HttpEntityEnclosingRequestBase;

public class HttpDeleteWithBody extends HttpEntityEnclosingRequestBase {

  public static final String METHOD_NAME = "DELETE";

  public String getMethod() {
    return METHOD_NAME;
  }

  public HttpDeleteWithBody(final String uri) {
    super();
    setURI(URI.create(uri));
  }

  public HttpDeleteWithBody(final URI uri) {
    super();
    setURI(uri);
  }

  public HttpDeleteWithBody() {
    super();
  }
}
