package com.purbon.kafka.topology.clients;

import static java.net.http.HttpRequest.BodyPublishers.noBody;
import static java.net.http.HttpRequest.BodyPublishers.ofString;

import com.purbon.kafka.topology.Configuration;
import com.purbon.kafka.topology.api.mds.Response;
import com.purbon.kafka.topology.utils.BasicAuth;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpRequest.BodyPublisher;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.KeyManagementException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertificateException;
import java.time.Duration;
import java.util.Optional;
import javax.net.ssl.KeyManager;
import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.TrustManagerFactory;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class JulieHttpClient {

  private static final Logger LOGGER = LogManager.getLogger(JulieHttpClient.class);

  private final long DEFAULT_TIMEOUT_MS = 60000;

  private HttpClient httpClient;
  protected final String server;
  private String token;

  public JulieHttpClient(String server) throws IOException {
    this(server, Optional.empty());
  }

  public JulieHttpClient(String server, Optional<Configuration> configOptional) throws IOException {
    this.server = server;
    this.token = "";
    this.httpClient = configureHttpOrHttpsClient(configOptional);
  }

  private HttpRequest.Builder setupARequest(String url, long timeoutMs) {
    HttpRequest.Builder builder =
        HttpRequest.newBuilder(URI.create(server + url))
            .timeout(Duration.ofMillis(timeoutMs))
            .header("accept", " application/json")
            .header("Content-Type", "application/json");
    if (!token.isBlank()) {
      builder = builder.header("Authorization", token);
    }
    return builder;
  }

  protected HttpClient configureHttpOrHttpsClient(Optional<Configuration> configOptional)
      throws IOException {
    if (configOptional.isEmpty()) {
      return HttpClient.newBuilder().build();
    }
    Configuration config = configOptional.get();

    SSLContext sslContext = null;

    try {
      sslContext = SSLContext.getInstance("TLS");

      if (areKeyStoreConfigured(config)) {
        var keyManagers = getKeyManagersFromKeyStore(config);
        var trustManagers = getTrustManagersFromTrustStore(config);
        sslContext.init(keyManagers, trustManagers, null);
      } else {
        LOGGER.debug("Keystore and Trusstore not configured, connection will be using plain HTTP");
        sslContext = SSLContext.getDefault();
      }

    } catch (KeyManagementException
        | NoSuchAlgorithmException
        | CertificateException
        | KeyStoreException
        | IOException
        | UnrecoverableKeyException e) {
      LOGGER.error(e);
      throw new IOException(e);
    }

    return HttpClient.newBuilder().sslContext(sslContext).build();
  }

  protected boolean areKeyStoreConfigured(Configuration config) {
    boolean existKeystore =
        config.getSslKeyStoreLocation().map(p -> Files.exists(Paths.get(p))).orElse(false);
    boolean existTruststore =
        config.getSslTrustStoreLocation().map(p -> Files.exists(Paths.get(p))).orElse(false);
    return existKeystore && existTruststore;
  }

  protected TrustManager[] getTrustManagersFromTrustStore(Configuration config)
      throws NoSuchAlgorithmException, CertificateException, KeyStoreException, IOException {
    TrustManagerFactory tmf = TrustManagerFactory.getInstance("PKIX");
    KeyStore ts =
        loadKeyStore(config.getSslTrustStoreLocation(), config.getSslTrustStorePassword());
    tmf.init(ts);
    return tmf.getTrustManagers();
  }

  protected KeyManager[] getKeyManagersFromKeyStore(Configuration config)
      throws NoSuchAlgorithmException, CertificateException, KeyStoreException, IOException,
          UnrecoverableKeyException {
    KeyManagerFactory kmf = KeyManagerFactory.getInstance("PKIX");
    KeyStore ks = loadKeyStore(config.getSslKeyStoreLocation(), config.getSslKeyStorePassword());
    kmf.init(ks, config.getSslKeyStorePassword().get().toCharArray());
    return kmf.getKeyManagers();
  }

  private KeyStore loadKeyStore(
      Optional<String> sslStoreLocation, Optional<String> sslStorePassword)
      throws CertificateException, NoSuchAlgorithmException, KeyStoreException, IOException {
    try {
      KeyStore ks = KeyStore.getInstance("PKCS12");
      char[] password = sslStorePassword.get().toCharArray();
      InputStream is = Files.newInputStream(Path.of(sslStoreLocation.get()));
      ks.load(is, password);
      return ks;
    } catch (KeyStoreException | IOException | NoSuchAlgorithmException | CertificateException ex) {
      LOGGER.error(ex);
      throw ex;
    }
  }

  public void setBasicAuth(BasicAuth basicAuth) {
    this.token = basicAuth.toHttpAuthToken();
  }

  public Response doGet(String url) throws IOException {
    HttpRequest request = getRequest(url, DEFAULT_TIMEOUT_MS);
    return doGet(request);
  }

  private HttpRequest getRequest(String url, long timeoutMs) {
    return setupARequest(url, timeoutMs).GET().build();
  }

  protected Response doGet(HttpRequest request) throws IOException {
    LOGGER.debug("method: " + request.method() + " request.uri: " + request.uri());
    try {
      HttpResponse<String> response =
          httpClient.send(request, HttpResponse.BodyHandlers.ofString());
      LOGGER.debug("method: " + request.method() + " response: " + response);
      return new Response(response);
    } catch (Exception ex) {
      throw new IOException(ex);
    }
  }

  public String doPost(String url, String body) throws IOException {
    LOGGER.debug("doPost: " + url + " body: " + body);
    HttpRequest request = postRequest(url, body, DEFAULT_TIMEOUT_MS);
    return doRequest(request);
  }

  private HttpRequest postRequest(String url, String body, long timeoutMs) {
    return setupARequest(url, timeoutMs).POST(ofString(body)).build();
  }

  protected void doPut(String url) throws IOException {
    LOGGER.debug("doPut: " + url);
    HttpRequest request = putRequest(url, DEFAULT_TIMEOUT_MS);
    doRequest(request);
  }

  protected String doPut(String url, String body) throws IOException {
    LOGGER.debug("doPut: " + url + " body: " + body);
    HttpRequest request = putRequest(url, ofString(body), DEFAULT_TIMEOUT_MS);
    return doRequest(request);
  }

  private HttpRequest putRequest(String url, long timeoutMs) {
    return putRequest(url, noBody(), timeoutMs);
  }

  private HttpRequest putRequest(String url, BodyPublisher bodyPublisher, long timeoutMs) {
    return setupARequest(url, timeoutMs).PUT(bodyPublisher).build();
  }

  public void doDelete(String url) throws IOException {
    doDelete(url, "");
  }

  public void doDelete(String url, String body) throws IOException {
    LOGGER.debug("doDelete: " + url + " body: " + body);
    HttpRequest request = deleteRequest(url, body, DEFAULT_TIMEOUT_MS);
    doRequest(request);
  }

  private HttpRequest deleteRequest(String url, String body, long timeoutMs) {
    HttpRequest.Builder builder = setupARequest(url, timeoutMs);
    BodyPublisher bodyPublisher = !body.isEmpty() ? ofString(body) : noBody();
    builder = builder.method("DELETE", bodyPublisher);
    return builder.build();
  }

  private String doRequest(HttpRequest request) throws IOException {
    LOGGER.debug("method: " + request.method() + " request.uri: " + request.uri());
    String result = "";
    try {
      HttpResponse<String> response =
          httpClient.send(request, HttpResponse.BodyHandlers.ofString());
      LOGGER.debug("method: " + request.method() + " response: " + response);
      int statusCode = response.statusCode();
      if (statusCode < 200 || statusCode > 299) {
        String body = response.body() != null ? response.body() : "";
        throw new IOException(
            "Something happened with the connection, response status code: "
                + statusCode
                + " body: "
                + body);
      }

      if (response.body() != null) {
        result = response.body();
      }
    } catch (Exception ex) {
      throw new IOException(ex);
    }
    return result;
  }

  public String baseUrl() {
    return server;
  }
}
