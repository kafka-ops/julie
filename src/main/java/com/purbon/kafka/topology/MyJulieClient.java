package com.purbon.kafka.topology;

import com.purbon.kafka.topology.api.mds.Response;
import com.purbon.kafka.topology.clients.JulieHttpClient;
import java.io.IOException;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

public class MyJulieClient {

  private static TrustManager[] trustAllCerts =
      new TrustManager[] {
        new X509TrustManager() {
          public java.security.cert.X509Certificate[] getAcceptedIssuers() {
            return null;
          }

          public void checkClientTrusted(
              java.security.cert.X509Certificate[] certs, String authType) {}

          public void checkServerTrusted(
              java.security.cert.X509Certificate[] certs, String authType) {}
        }
      };

  public static class MyClient extends JulieHttpClient {

    public MyClient(String server) {
      super(server);
    }

    public Response doGet() throws IOException {
      return doGet("/");
    }
  }

  public static void main(String[] args)
      throws IOException, InterruptedException, NoSuchAlgorithmException, KeyManagementException {

    var client = new MyClient("https://localhost:18083");
    var response = client.doGet();
    System.out.println(response.getResponseAsString());
  }
}
