package org.zeroturnaround.jenkins.reporter;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathFactory;

import org.xml.sax.SAXException;

public class JenkinsHelperBuilder {

  public JenkinsViewAnalyser createDefault(final String userName, final String apiToken) {
    return createDefault(userName, apiToken, null, false);
  }

  public JenkinsViewAnalyser createDefault(final String userName, final String apiToken, final Integer sslPort, final boolean ignoreSslCertificate) {
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

    final DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();

    DocumentBuilder builder;
    try {
      builder = factory.newDocumentBuilder();
    }
    catch (ParserConfigurationException e) {
      throw new ProcessingException(e);
    }
    final XPathFactory xPathfactory = XPathFactory.newInstance();
    XPath xpath = xPathfactory.newXPath();

    JenkinsHttpClient httpClient;

    if (ignoreSslCertificate && sslPort != null) {
        httpClient = new JenkinsIgnoreSslClient(userName, apiToken, sslPort);
    }
    else {
        httpClient = new JenkinsHttpClient(userName, apiToken);
    }

    return new JenkinsViewAnalyser(builder, xpath, saxParser, httpClient);
  }
}
