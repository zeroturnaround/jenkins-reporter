/**
 *    Copyright (C) 2013 ZeroTurnaround LLC <support@zeroturnaround.com>
 *
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 */
package org.zeroturnaround.jenkins.reporter;

import java.io.IOException;
import java.net.URI;
import java.util.ArrayList;
import java.util.Collection;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathFactory;

import org.apache.http.HttpException;
import org.apache.http.HttpHost;
import org.apache.http.HttpRequest;
import org.apache.http.HttpRequestInterceptor;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.AuthState;
import org.apache.http.auth.Credentials;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.protocol.ClientContext;
import org.apache.http.impl.auth.BasicScheme;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.protocol.ExecutionContext;
import org.apache.http.protocol.HttpContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;
import org.zeroturnaround.jenkins.reporter.model.Job;
import org.zeroturnaround.jenkins.reporter.model.View;
import org.zeroturnaround.jenkins.reporter.util.URLParamEncoder;
import org.zeroturnaround.jenkins.reporter.util.XMLResponseHandler;

public class JenkinsService {
  private static final Logger log = LoggerFactory.getLogger(JenkinsService.class); //NOSONAR

  private static final String JENKINS_URL_PROPERTY = "jenkins.url";
  private static final String JENKINS_VIEW_URL_PATTERN_PROPERTY = "jenkins.view.url.pattern";
  private static final String JENKINS_JOB_URL_PATTERN_PROPERTY = "jenkins.job.url.patter";

  private final String jenkinsUrl = System.getProperty(JENKINS_URL_PROPERTY, "http://wraith/jenkins/");
  private final String jenkinsViewUrlPattern = System.getProperty(JENKINS_VIEW_URL_PATTERN_PROPERTY, "%sview/LiveRebel/view/%s");
  private final String jenkinsJobUrlPattern = System.getProperty(JENKINS_JOB_URL_PATTERN_PROPERTY, "%sjob/%s");

  private final DefaultHttpClient httpClient;
  private XMLResponseHandler handler;
  private final XPath xpath;

  private HttpRequestInterceptor preemptiveAuth = new HttpRequestInterceptor() {
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

  public JenkinsService(String username, String authToken) throws Exception {
    DocumentBuilder builder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
    handler = new XMLResponseHandler(builder);
    xpath = XPathFactory.newInstance().newXPath();

    httpClient = new DefaultHttpClient();
    Credentials credentials = new UsernamePasswordCredentials(username, authToken);
    httpClient.getCredentialsProvider().setCredentials(AuthScope.ANY, credentials);
    httpClient.addRequestInterceptor(preemptiveAuth, 0);
  }

  public Document parseXML(String uri) throws IOException {
    log.trace("Requesting url {} ...", uri);
    HttpGet get = new HttpGet(uri);
    return httpClient.execute(get, handler);
  }

  public View readView(String viewName) throws Exception {
    final Document doc = parseXML(getViewURI(viewName) + "/api/xml");

    final View view = new View();
    view.setUrl(new URI(xpath.compile("/listView/url").evaluate(doc)));
    view.setName(xpath.compile("/listView/name").evaluate(doc));

    Collection<Job> jobs = new ArrayList<Job>();
    NodeList jobNodes = doc.getElementsByTagName("job");
    for (int i = 0; i < jobNodes.getLength(); i++) {
      Node jobNode = jobNodes.item(i);

      Job job = new Job();
      job.setName(xpath.compile("name").evaluate(jobNode));
      job.setUrl(new URI(xpath.compile("url").evaluate(jobNode)));
      job.setColor(xpath.compile("color").evaluate(jobNode));
      job.setConfigXml(fetchConfigXml(job));
      jobs.add(job);
    }
    view.setJobs(jobs);

    return view;
  }

  private Document fetchConfigXml(Job job) throws SAXException, IOException {
    return parseXML(getJobURI(job.getName()) + "/config.xml");
  }

  private String getViewURI(String viewName) {
    String uri = String.format(jenkinsViewUrlPattern, jenkinsUrl, URLParamEncoder.encode(viewName));
    return uri;
  }

  private String getJobURI(String jobName) {
    String uri = String.format(jenkinsJobUrlPattern, jenkinsUrl, URLParamEncoder.encode(jobName));
    return uri;
  }
}
