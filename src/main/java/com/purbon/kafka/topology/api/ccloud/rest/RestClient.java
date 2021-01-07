package com.purbon.kafka.topology.api.ccloud.rest;

import org.apache.http.client.HttpClient;
import org.apache.http.client.fluent.*;
import org.apache.http.client.methods.HttpEntityEnclosingRequestBase;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.config.Registry;
import org.apache.http.config.RegistryBuilder;
import org.apache.http.conn.socket.ConnectionSocketFactory;
import org.apache.http.conn.socket.LayeredConnectionSocketFactory;
import org.apache.http.conn.socket.PlainConnectionSocketFactory;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.conn.ssl.SSLInitializationException;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;

import javax.net.ssl.SSLContext;
import java.io.IOException;
import java.net.URI;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;

public class RestClient {

    final static PoolingHttpClientConnectionManager CONNMGR;
    final static HttpClient CLIENT;

    static {
        LayeredConnectionSocketFactory ssl = null;
        try {
            ssl = SSLConnectionSocketFactory.getSystemSocketFactory();
        } catch (final SSLInitializationException ex) {
            final SSLContext sslcontext;
            try {
                sslcontext = SSLContext.getInstance(SSLConnectionSocketFactory.TLS);
                sslcontext.init(null, null, null);
                ssl = new SSLConnectionSocketFactory(sslcontext);
            } catch (final SecurityException ignore) {
            } catch (final KeyManagementException ignore) {
            } catch (final NoSuchAlgorithmException ignore) {
            }
        }

        final Registry<ConnectionSocketFactory> sfr = RegistryBuilder.<ConnectionSocketFactory>create()
                .register("http", PlainConnectionSocketFactory.getSocketFactory())
                .register("https", ssl != null ? ssl : SSLConnectionSocketFactory.getSocketFactory())
                .build();

        CONNMGR = new PoolingHttpClientConnectionManager(sfr);
        CONNMGR.setDefaultMaxPerRoute(100);
        CONNMGR.setMaxTotal(200);
        CONNMGR.setValidateAfterInactivity(1000);
        CLIENT = HttpClientBuilder.create()
                .setConnectionManager(CONNMGR)
                .build();
    }

    public static Content execute(Request request) throws IOException {
        return Executor.newInstance(CLIENT).execute(request).returnContent();
    }

    public static Content execute(HttpUriRequest request) throws IOException {
        return CLIENT.execute(request, new ContentResponseHandler());
    }

    public static DeleteBody DeleteBody(String uri) {
        return new DeleteBody(uri);
    }

    public static DeleteBody DeleteBody(URI uri) {
        return new DeleteBody(uri);
    }

    static class DeleteBody extends HttpEntityEnclosingRequestBase {

        public static final String METHOD_NAME = "DELETE";

        public String getMethod() {
            return METHOD_NAME;
        }

        public DeleteBody(final String uri) {
            setURI(URI.create(uri));
        }

        public DeleteBody(final URI uri) {
            setURI(uri);
        }

        public DeleteBody bodyString(final String body, final ContentType contentType) {
            setEntity(new StringEntity(body, ContentType.APPLICATION_JSON));
            return this;
        }
    }

}
