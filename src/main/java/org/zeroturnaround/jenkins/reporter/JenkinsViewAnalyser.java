package org.zeroturnaround.jenkins.reporter;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.Date;
import java.util.Iterator;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.SAXParser;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpression;
import javax.xml.xpath.XPathExpressionException;

import org.apache.http.client.utils.HttpClientUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.DOMException;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;
import org.zeroturnaround.jenkins.reporter.model.Build;
import org.zeroturnaround.jenkins.reporter.model.JenkinsView;
import org.zeroturnaround.jenkins.reporter.model.Job;
import org.zeroturnaround.jenkins.reporter.model.TestReport;

public class JenkinsViewAnalyser {
  private static final Logger log = LoggerFactory.getLogger(JenkinsViewAnalyser.class); // NOSONAR
  private DocumentBuilder builder;
  private final XPath xpath;
  private final SAXParser saxParser;
  private final JenkinsHttpClient jhc;

  private static final int SECONDS_IN_MINUTE = 60;
  private static final int SECONDS_IN_HOUR = SECONDS_IN_MINUTE * 60;
  private static final int MILLISECONDS_IN_SECOND = 1000;

  public JenkinsViewAnalyser(DocumentBuilder builder, XPath xpath, SAXParser saxParser, JenkinsHttpClient jhc) {
    this.builder = builder;
    this.xpath = xpath;
    this.saxParser = saxParser;
    this.jhc = jhc;
  }

  public JenkinsView getViewData(URI viewUrl) {
    JenkinsView viewData = new JenkinsView();

    viewData.setName(getViewName(viewUrl));
    viewData.setUrl(getViewURL(viewUrl));
    viewData.setJobsTotal(getJobCount(viewUrl));
    viewData.setJobs(readJobs(viewUrl));

    return viewData;
  }

  private URI getViewURL(URI viewUrl) {
    try {

      Document doc = jhc.fetchAsXMLDocument(viewUrl.toASCIIString() + "/api/xml?tree=name,url");
      URI uri = new URI(doc.getElementsByTagName("url").item(0).getTextContent());
      return uri;
    }
    catch (DOMException e) {
      throw new ProcessingException(e);
    }
    catch (URISyntaxException e) {
      throw new ProcessingException(e);
    }
  }

  private String getViewName(URI viewUrl) {
    Document doc;
    doc = jhc.fetchAsXMLDocument(viewUrl.toASCIIString() + "/api/xml?tree=name,url");
    return doc.getElementsByTagName("name").item(0).getTextContent();
  }

  private int getJobCount(URI uri) {
    final String fullUrl = uri.toASCIIString() + "/api/xml?wrapper=jobs&tree=jobs[name,url,color]";

    log.debug("Counting total number of jobs for '{}'", fullUrl);

    NodeList nodes;
    try {
      Document doc = jhc.fetchAsXMLDocument(fullUrl);
      final XPathExpression expr = xpath.compile("//job");
      nodes = (NodeList) expr.evaluate(doc, XPathConstants.NODESET);
    }
    catch (XPathExpressionException e) {
      throw new ProcessingException(e);
    }

    return nodes.getLength();
  }

  private Collection<Job> readJobs(URI uri) {
    final String fullUrl = uri.toASCIIString() + "/api/xml?xpath=//job&wrapper=jobs&tree=jobs[name,url,color]";

    log.debug("Reading information about failing jobs for '{}'", fullUrl);

    Document doc = jhc.fetchAsXMLDocument(fullUrl);

    final Collection<Job> jobs = parseJobsFromXml(doc, "job", true);

    // call jenkins after parsing xml
    log.info("Fetching last completed build info for " + jobs.size() + " jobs");

    for (final Iterator<Job> iter = jobs.iterator(); iter.hasNext();) {
      final Job job = iter.next();
      job.setLastCompletedBuild(getLastCompletedBuild(job));
      job.setChildren(readChildrenJobs(job));

      if (job.getLastCompletedBuild() == null && job.getChildren().isEmpty()) {
        // job has no last completed build nor children, ignore it
        iter.remove();
      }
    }

    return jobs;
  }

  private Collection<Job> readChildrenJobs(Job parentJob) {
    final String uri = parentJob.getUrl().toASCIIString() + "/api/xml?xpath=/matrixProject/activeConfiguration&wrapper=activeConfigurations";

    log.debug("Reading child jobs of matrix job '{}' at '{}'", parentJob.getName(), uri.toString());
    final Document doc = jhc.fetchAsXMLDocument(uri);

    final Collection<Job> jobs = parseJobsFromXml(doc, "activeConfiguration", false);

    if (!jobs.isEmpty()) {
      log.info("Fetching last completed build info for " + jobs.size() + " child jobs of " + parentJob.getName() + "...");

      for (final Iterator<Job> iter = jobs.iterator(); iter.hasNext();) {
        final Job job = iter.next();
        try {
          Build lastCompletedBuild = getLastCompletedBuild(job);
          job.setLastCompletedBuild(lastCompletedBuild);
        }
        // sometimes there is no last completed build
        // we can ignore the job
        catch (DocumentNotFoundException e) {
          job.setLastCompletedBuild(null);
          iter.remove();
        }
      }
    }

    return jobs;
  }

  private Collection<Job> parseJobsFromXml(final Document doc, final String jobNodeName, final boolean filterByPrefix) {
    NodeList nodes;
    try {
      final XPathExpression expr = xpath.compile("//" + jobNodeName);
      nodes = (NodeList) expr.evaluate(doc, XPathConstants.NODESET);
    }
    catch (XPathExpressionException e) {
      throw new ProcessingException(e);
    }

    final Collection<Job> jobs = new ArrayList<Job>();

    for (int i = 0; i < nodes.getLength(); i++) {
      final Node node = nodes.item(i);

      if (node.getNodeType() == Node.ELEMENT_NODE) {
        final Element el = (Element) node;
        final String name = el.getElementsByTagName("name").item(0).getTextContent();

        if (filterByPrefix && Main.JOB_NAME_PREFIX != null && !name.startsWith(Main.JOB_NAME_PREFIX)) {
          continue;
        }

        // these listings also list "Groups" that are not actually jobs
        // I'll just ignore these
        if (el.getElementsByTagName("color") == null) {
          continue;
        }

        final Job job = new Job();
        job.setName(name);
        try {
          String jobUrl = el.getElementsByTagName("url").item(0).getTextContent();
          job.setUrl(new URI(jobUrl));
        }
        catch (DOMException e) {
          throw new ProcessingException(e);
        }
        catch (URISyntaxException e) {
          throw new ProcessingException(e);
        }

        job.setColor(el.getElementsByTagName("color").item(0).getTextContent());
        jobs.add(job);
      }
    }
    return jobs;
  }

  private Build getLastCompletedBuild(Job job) {
    Build build = null;

    log.debug("Fetching last completed build info for job {}", job.getName());
    final String uri = job.getUrl() + "lastCompletedBuild/api/xml?tree=number,url,timestamp,duration,result";
    final Document doc = jhc.fetchAsXMLDocument(uri);

    build = new Build();
    build.setId(Integer.parseInt(doc.getElementsByTagName("number").item(0).getTextContent()));
    build.setResult(doc.getElementsByTagName("result").item(0).getTextContent());
    try {
      build.setUrl(new URI(doc.getElementsByTagName("url").item(0).getTextContent()));
      build.setTestReport(readTestReport(build.getUrl()));
    }
    catch (DOMException e) {
      throw new ProcessingException(e);
    }
    catch (URISyntaxException e) {
      throw new ProcessingException(e);
    }
    catch (SAXException e) {
      throw new ProcessingException(e);
    }
    catch (IOException e) {
      throw new ProcessingException(e);
    }
    build.setTimestamp(new Date(Long.parseLong(doc.getElementsByTagName("timestamp").item(0).getTextContent())));
    Calendar cal = Calendar.getInstance();
    cal.setTime(build.getTimestamp());
    // FIXME this will cause confusion in the beginning of Jan
    build.setDayOfYear(cal.get(Calendar.DAY_OF_YEAR));

    final long durationSeconds = Long.parseLong(doc.getElementsByTagName("duration").item(0).getTextContent()) / MILLISECONDS_IN_SECOND;
    build.setDuration(String.format("%d:%02d:%02d", durationSeconds / SECONDS_IN_HOUR, durationSeconds % SECONDS_IN_HOUR / SECONDS_IN_MINUTE, durationSeconds % SECONDS_IN_MINUTE));

    return build;
  }

  private TestReport readTestReport(URI buildUrl) throws SAXException, IOException {

    final TestReport testReport = new TestReport();

    final DefaultHandler handler = new ReadTestReportHandler(testReport);

    try {
      InputStream is = jhc.fetchAsInputStream(buildUrl + "testReport/api/xml");
      saxParser.parse(is, handler);
    }
    catch (FileNotFoundException e) {
      log.debug("No test report available for {}", buildUrl);
      return null;
    }

    return testReport;
  }

}
