package org.zeroturnaround.jenkins.reporter;

import org.apache.http.conn.scheme.Scheme;
import org.apache.http.conn.ssl.SSLSocketFactory;
import org.apache.http.conn.ssl.TrustStrategy;

import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;

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

            httpClient.getConnectionManager().getSchemeRegistry().register(new Scheme("https", sslPort == null || sslPort == -1 ? 443 : sslPort, sslsf));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
