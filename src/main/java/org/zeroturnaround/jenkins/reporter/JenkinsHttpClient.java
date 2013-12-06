package org.zeroturnaround.jenkins.reporter;

import java.io.IOException;
import java.io.InputStream;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.apache.http.HttpException;
import org.apache.http.HttpHost;
import org.apache.http.HttpRequest;
import org.apache.http.HttpRequestInterceptor;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.AuthState;
import org.apache.http.auth.Credentials;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.client.HttpResponseException;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.protocol.ClientContext;
import org.apache.http.impl.auth.BasicScheme;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.protocol.ExecutionContext;
import org.apache.http.protocol.HttpContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.zeroturnaround.jenkins.reporter.util.XMLResponseHandler;

public class JenkinsHttpClient {
  private static final Logger log = LoggerFactory.getLogger(JenkinsHttpClient.class); // NOSONAR

  protected final DefaultHttpClient httpClient;
  private XMLResponseHandler handler;

  HttpRequestInterceptor preemptiveAuth = new HttpRequestInterceptor() {
    public void process(final HttpRequest request, final HttpContext context) throws HttpException, IOException {
      AuthState authState = (AuthState) context.getAttribute(ClientContext.TARGET_AUTH_STATE);
      CredentialsProvider credsProvider = (CredentialsProvider) context.getAttribute(ClientContext.CREDS_PROVIDER);
      HttpHost targetHost = (HttpHost) context.getAttribute(ExecutionContext.HTTP_TARGET_HOST);

      if (authState.getAuthScheme() == null) {
        AuthScope authScope = new AuthScope(targetHost.getHostName(), targetHost.getPort());
        Credentials creds = credsProvider.getCredentials(authScope);
        if (creds != null) {
          authState.update(new BasicScheme(), creds);
        }
      }
    }
  };

  public JenkinsHttpClient(String username, String authToken) {
    DocumentBuilder builder;
    try {
      builder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
    }
    catch (ParserConfigurationException e) {
      throw new ProcessingException(e);
    }
    handler = new XMLResponseHandler(builder);

    httpClient = new DefaultHttpClient();
    if (username != null) {
      Credentials credentials = new UsernamePasswordCredentials(username, authToken);
      httpClient.getCredentialsProvider().setCredentials(AuthScope.ANY, credentials);
      httpClient.addRequestInterceptor(preemptiveAuth, 0);
    }
  }

  public Document fetchAsXMLDocument(String uri) {
    log.trace("Requesting url {} ...", uri);
    HttpGet get = new HttpGet(uri);
    try {
      return httpClient.execute(get, handler);
    }
    catch (HttpResponseException e) {
      if (e.getStatusCode() == 404)
        throw new DocumentNotFoundException(e);
      else
        throw new ProcessingException(e);
    }
    catch (IOException e) {
      throw new ProcessingException(e);
    }
  }

  public InputStream fetchAsInputStream(String uri) {
    log.trace("Requesting url {} ...", uri);
    HttpGet get = new HttpGet(uri);
    try {
      return httpClient.execute(get).getEntity().getContent();
    }
    catch (ClientProtocolException e) {
      throw new ProcessingException(e);
    }
    catch (IOException e) {
      throw new ProcessingException(e);
    }
  }
}
