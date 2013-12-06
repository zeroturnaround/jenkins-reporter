package org.zeroturnaround.jenkins.reporter;

import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;

import org.apache.http.conn.scheme.Scheme;
import org.apache.http.conn.ssl.SSLSocketFactory;
import org.apache.http.conn.ssl.TrustStrategy;

public class JenkinsIgnoreSslClient extends JenkinsHttpClient {
  public JenkinsIgnoreSslClient(String username, String authToken, Integer sslPort) {
    super(username, authToken);

    try {
      SSLSocketFactory sslsf = new SSLSocketFactory(new TrustStrategy() {

        public boolean isTrusted(final X509Certificate[] chain, String authType) throws CertificateException {
          // Oh, I am easy...
          return true;
        }

      });

      httpClient.getConnectionManager().getSchemeRegistry().register(new Scheme("https", sslPort, sslsf));
    }
    catch (Exception e) {
      e.printStackTrace();
    }
  }
}
