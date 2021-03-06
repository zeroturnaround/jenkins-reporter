package org.zeroturnaround.jenkins.reporter;

import java.net.URI;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathFactory;

import org.xml.sax.SAXException;

public class JenkinsHelperBuilder {

  public JenkinsViewAnalyser createDefault(URI viewUrl, final String userName, final String apiToken, final boolean ignoreSslCertificate) {
    final SAXParserFactory saxFactory = SAXParserFactory.newInstance();
    SAXParser saxParser;
    try {
      saxParser = saxFactory.newSAXParser();
    }
    catch (ParserConfigurationException e) {
      throw new ProcessingException(e);
    }
    catch (SAXException e) {
      throw new ProcessingException(e);
    }

    final XPathFactory xPathfactory = XPathFactory.newInstance();
    XPath xpath = xPathfactory.newXPath();

    JenkinsHttpClient httpClient;

    if (ignoreSslCertificate && "https".equalsIgnoreCase(viewUrl.getScheme())) {
      // it seems that 443 is denoted by -1 when coming from uri
      int port = viewUrl.getPort();
      if (port == -1) {
        port = 443;
      }

      httpClient = new JenkinsIgnoreSslClient(userName, apiToken, port);
    }
    else {
      httpClient = new JenkinsHttpClient(userName, apiToken);
    }

    return new JenkinsViewAnalyser(xpath, saxParser, httpClient);
  }
}
