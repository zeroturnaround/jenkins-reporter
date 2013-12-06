package org.zeroturnaround.jenkins.reporter;

import java.io.IOException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;

import javax.net.ssl.SSLException;
import javax.net.ssl.SSLSession;
import javax.net.ssl.SSLSocket;

import org.apache.http.conn.scheme.Scheme;
import org.apache.http.conn.ssl.SSLSocketFactory;
import org.apache.http.conn.ssl.TrustStrategy;
import org.apache.http.conn.ssl.X509HostnameVerifier;

public class JenkinsIgnoreSslClient extends JenkinsHttpClient {
  public JenkinsIgnoreSslClient(String username, String authToken, Integer sslPort) {
    super(username, authToken);

    try {
      SSLSocketFactory sslsf = new SSLSocketFactory(new TrustStrategy() {

        public boolean isTrusted(final X509Certificate[] chain, String authType) throws CertificateException {
          // Oh, I am easy...
          return true;
        }

      },
          new X509HostnameVerifier() {

            @Override
            public boolean verify(String arg0, SSLSession arg1) {
              return true;
            }

            @Override
            public void verify(String host, SSLSocket ssl) throws IOException {
            }

            @Override
            public void verify(String host, X509Certificate cert) throws SSLException {
            }

            @Override
            public void verify(String host, String[] cns, String[] subjectAlts) throws SSLException {
            }

          });

      httpClient.getConnectionManager().getSchemeRegistry().register(new Scheme("https", sslPort, sslsf));
    }
    catch (Exception e) {
      e.printStackTrace();
    }
  }
}
