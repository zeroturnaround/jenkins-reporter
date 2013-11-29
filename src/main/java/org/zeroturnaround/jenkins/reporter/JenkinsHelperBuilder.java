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
  public JenkinsViewAnalyser createDefault() {
    final SAXParserFactory saxFactory = SAXParserFactory.newInstance();
    SAXParser saxParser;
    try {
      saxParser = saxFactory.newSAXParser();
    }
    catch (ParserConfigurationException e) {
      throw new RuntimeException(e);
    }
    catch (SAXException e) {
      throw new RuntimeException(e);
    }

    final DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();

    DocumentBuilder builder;
    try {
      builder = factory.newDocumentBuilder();
    }
    catch (ParserConfigurationException e) {
      throw new RuntimeException(e);
    }
    final XPathFactory xPathfactory = XPathFactory.newInstance();
    XPath xpath = xPathfactory.newXPath();

    JenkinsViewAnalyser helper = new JenkinsViewAnalyser(builder, xpath, saxParser);
    return helper;
  }
}
