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
package com.zeroturnaround.liverebel;

import static com.google.common.collect.Lists.newArrayList;

import java.awt.Desktop;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.net.URI;
import java.net.URISyntaxException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpression;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;

import org.apache.velocity.Template;
import org.apache.velocity.VelocityContext;
import org.apache.velocity.app.VelocityEngine;
import org.apache.velocity.runtime.RuntimeConstants;
import org.apache.velocity.runtime.resource.loader.ClasspathResourceLoader;
import org.apache.velocity.tools.generic.DateTool;
import org.apache.velocity.tools.generic.EscapeTool;
import org.apache.velocity.tools.generic.NumberTool;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.DOMException;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

import com.zeroturnaround.jenkins.model.Build;
import com.zeroturnaround.jenkins.model.Job;
import com.zeroturnaround.jenkins.model.TestReport;
import com.zeroturnaround.jenkins.model.View;
import com.zeroturnaround.jenkins.util.CommentsHelper;
import com.zeroturnaround.jenkins.util.URLParamEncoder;

public class JenkinsReporter {

  private static final String JENKINS_URL_PROPERTY = "jenkins.url";
  private static final String JENKINS_VIEW_URL_PATTERN_PROPERTY = "jenkins.view.url.pattern";
  private static final String OUTPUT_FILE_PROPERTY = "output.file";

  public static final class JobByTimestampComparator implements Comparator<Job> {
    @Override
    public int compare(Job job1, Job job2) {
      if (job1.getLastCompletedBuild() != null && job2.getLastCompletedBuild() != null) {
        return -1 * job1.getLastCompletedBuild().getTimestamp().compareTo(job2.getLastCompletedBuild().getTimestamp());
      }
      else {
        return -1;
      }
    }
  }

  private static final String JOB_NAME_PREFIX = System.getProperty("jobName.prefix");
  private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(JenkinsReporter.class);

  // more relaxed file format - only "=" sign should be escaped in the key
  // with "\"
  private static CommentsHelper createCommentsHelper(final String jenkinsViewName) throws IOException {
    final String failureCommentsDir = "src/main/resources";
    final File file = new File(failureCommentsDir, "failureComments-" + jenkinsViewName + ".properties");
    return new CommentsHelper().load(file);
  }

  public static final void main(String[] args) throws Exception {
    if (args.length == 0) {
      System.err.println("Please provide command.");
      System.exit(-1);
    }

    if (args[0].equals("diff")) {
      DiffConfigurations.diffConfigurations(args);
    }
    else {
      generateTestReport(args);
    }
  }

  public static final void generateTestReport(String jenkinsViewNames[]) throws URISyntaxException, IOException, ParserConfigurationException, SAXException,
      XPathExpressionException {
    if (jenkinsViewNames.length == 0) {
      System.err.println("Please give the name of Jenkins view as parameter to this script.");
      System.exit(-1);
    }

    final SimpleDateFormat sdf = new SimpleDateFormat("'jenkins-report-'yyyy-MM-dd_HH.mm.ss");
    String jenkinsUrl = getProperty(JENKINS_URL_PROPERTY, "http://jenkins/jenkins/");
    if (!jenkinsUrl.endsWith("/"))
      jenkinsUrl = jenkinsUrl + "/";
    String jenkinsViewUrlPattern = getProperty(JENKINS_VIEW_URL_PATTERN_PROPERTY, "%sview/LiveRebel/view/%s");
    String outputFilename = getProperty(OUTPUT_FILE_PROPERTY, null);
    for (final String jenkinsViewName : jenkinsViewNames) {
      final URI viewUrl = new URI(String.format(jenkinsViewUrlPattern, jenkinsUrl, URLParamEncoder.encode(jenkinsViewName)));
      if (outputFilename != null) {
        log.info("using view URL {} and generating output to {}", viewUrl, outputFilename);
      }

      final JenkinsReporter app = new JenkinsReporter();

      if (Desktop.isDesktopSupported()) {
        if (outputFilename == null)
          outputFilename = jenkinsViewName + "-" + sdf.format(new Date()) + "-";
        final File outputFile = File.createTempFile(outputFilename, ".html");
        final PrintWriter out = new PrintWriter(new FileWriter(outputFile));

        app.generateReport(viewUrl, out);
        log.info("Generated report to: " + outputFile);

        Desktop.getDesktop().browse(outputFile.toURI());
      }
      else {
        final ch.qos.logback.classic.Logger root = (ch.qos.logback.classic.Logger) LoggerFactory.getLogger(Logger.ROOT_LOGGER_NAME);
        root.setLevel(ch.qos.logback.classic.Level.OFF);

        final PrintWriter out = outputFilename != null ? new PrintWriter(outputFilename, "UTF-8") : new PrintWriter(System.out);
        app.generateReport(viewUrl, out);
      }
    }
  }

  private static String getProperty(String propName, String defaultValue) {
    String value = System.getProperty(propName);
    if (value == null)
      return defaultValue;
    return value;
  }

  private final DocumentBuilder builder;
  private final DocumentBuilderFactory factory;
  private final SAXParserFactory saxFactory;
  private final SAXParser saxParser;
  private final Template template;
  private View view;
  private final XPath xpath;
  private final XPathFactory xPathfactory;

  public JenkinsReporter() throws ParserConfigurationException, SAXException {
    saxFactory = SAXParserFactory.newInstance();
    saxParser = saxFactory.newSAXParser();
    factory = DocumentBuilderFactory.newInstance();
    builder = factory.newDocumentBuilder();
    xPathfactory = XPathFactory.newInstance();
    xpath = xPathfactory.newXPath();

    final VelocityEngine velocityEngine = new VelocityEngine();
    velocityEngine.setProperty(RuntimeConstants.RESOURCE_LOADER, "classpath");
    velocityEngine.setProperty("classpath.resource.loader.class", ClasspathResourceLoader.class.getName());
    velocityEngine.init();

    template = velocityEngine.getTemplate("report.vm");
  }

  private int countJobsTotal() throws SAXException, IOException, XPathExpressionException {
    final String infoMsg = "Counting total number of jobs in view '" + view.getName() + "'";
    final String uri = view.getUrl().toASCIIString() + "/api/xml?wrapper=jobs&tree=jobs[name,url,color]";
    final Document doc = parseUri(uri, infoMsg);

    final XPathExpression expr = xpath.compile("//job");
    final NodeList nodes = (NodeList) expr.evaluate(doc, XPathConstants.NODESET);

    return nodes.getLength();
  }

  private void generateReport(PrintWriter out) throws IOException {
    final List<Job> failedJobs = newArrayList();
    failedJobs.addAll(view.getFailedJobs());

    // sort jobs by build timestamp
    Collections.sort(failedJobs, new JobByTimestampComparator());

    final List<Job> passedJobs = newArrayList();
    passedJobs.addAll(view.getPassedJobs());

    // sort jobs by build timestamp
    Collections.sort(passedJobs, new JobByTimestampComparator());

    final VelocityContext context = new VelocityContext();

    final DateTool dateTool = new DateTool();
    final Map<String, String> config = new HashMap<String, String>();
    config.put("timezone", "Europe/Tallinn");
    dateTool.configure(config);
    context.put("dateTool", dateTool);

    final NumberTool numberTool = new NumberTool();
    numberTool.configure(config);
    context.put("numberTool", numberTool);

    final EscapeTool escapeTool = new EscapeTool();
    context.put("escapeTool", escapeTool);

    context.put("view", view);
    context.put("failedJobs", failedJobs);
    context.put("passedJobs", passedJobs);
    context.put("commentsHelper", createCommentsHelper(view.getName()));

    final StringWriter writer = new StringWriter();
    template.merge(context, writer);

    out.println(writer.toString());

    out.flush();
    out.close();
  }

  private void generateReport(URI viewUrl, PrintWriter out) throws XPathExpressionException, URISyntaxException, SAXException, IOException,
      ParserConfigurationException {
    read(viewUrl);

    generateReport(out);
  }

  private Build getLastCompletedBuild(Job job) throws ParserConfigurationException, SAXException, IOException, URISyntaxException, XPathExpressionException {
    final String infoMsg = "Fetching last completed build info for job " + job.getName();
    Build build = null;

    try {
      final Document doc = parseUri(job.getUrl() + "lastCompletedBuild/api/xml?tree=number,url,timestamp,duration,result", infoMsg);

      build = new Build();
      build.setId(Integer.parseInt(doc.getElementsByTagName("number").item(0).getTextContent()));
      build.setResult(doc.getElementsByTagName("result").item(0).getTextContent());
      build.setUrl(new URI(doc.getElementsByTagName("url").item(0).getTextContent()));
      build.setTestReport(readTestReport(build.getUrl()));
      build.setTimestamp(new Date(Long.parseLong(doc.getElementsByTagName("timestamp").item(0).getTextContent())));
      Calendar cal = Calendar.getInstance();
      cal.setTime(build.getTimestamp());
      // FIXME this will cause confusion in the beginning of Jan
      build.setDayOfYear(cal.get(Calendar.DAY_OF_YEAR));

      final long durationSeconds = Long.parseLong(doc.getElementsByTagName("duration").item(0).getTextContent()) / 1000;
      build.setDuration(String.format("%d:%02d:%02d", durationSeconds / 3600, durationSeconds % 3600 / 60, durationSeconds % 60));
    }
    catch (final FileNotFoundException _ignore) {
      // this job has no last completed build, skip it
    }

    return build;
  }

  private Collection<Job> parseJobsFromXml(final Document doc, final String jobNodeName, final boolean filterByPrefix) throws XPathExpressionException, URISyntaxException {
    final XPathExpression expr = xpath.compile("//" + jobNodeName);
    final NodeList nodes = (NodeList) expr.evaluate(doc, XPathConstants.NODESET);

    final Collection<Job> jobs = new ArrayList<Job>();

    for (int i = 0; i < nodes.getLength(); i++) {
      final Node node = nodes.item(i);

      if (node.getNodeType() == Node.ELEMENT_NODE) {
        final Element el = (Element) node;
        final String name = el.getElementsByTagName("name").item(0).getTextContent();

        if (filterByPrefix && JOB_NAME_PREFIX != null && !name.startsWith(JOB_NAME_PREFIX)) {
          continue;
        }

        final Job job = new Job();
        job.setName(name);
        job.setUrl(new URI(el.getElementsByTagName("url").item(0).getTextContent()));
        job.setColor(el.getElementsByTagName("color").item(0).getTextContent());

        jobs.add(job);
      }
    }
    return jobs;
  }

  private Document parseUri(String uri, String infoMsg) throws SAXException, IOException {
    if (infoMsg != null) {
      log.info(infoMsg);
    }

    final Document doc = builder.parse(uri);

    if (infoMsg != null && log.isDebugEnabled()) {
      log.debug("Done: " + infoMsg);
    }

    return doc;
  }

  private void read(URI viewUrl) throws URISyntaxException, SAXException, IOException, XPathExpressionException, ParserConfigurationException {
    view = readView(viewUrl);
    view.setJobsTotal(countJobsTotal());
    view.setJobs(readJobs());
  }

  private Collection<Job> readJobs() throws ParserConfigurationException, SAXException, IOException, XPathExpressionException, URISyntaxException {
    final String infoMsg = "Reading information about failing jobs in view '" + view.getName() + "'";
    final String uri = view.getUrl().toASCIIString() + "/api/xml?xpath=//job&wrapper=jobs&tree=jobs[name,url,color]";
    final Document doc = parseUri(uri, infoMsg);

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

  private Collection<Job> readChildrenJobs(Job parentJob) throws SAXException, IOException, XPathExpressionException, URISyntaxException,
      ParserConfigurationException {
    final String infoMsg = "Reading child jobs of matrix job '" + parentJob.getName() + "'...";
    final String uri = parentJob.getUrl().toASCIIString() + "/api/xml?xpath=/matrixProject/activeConfiguration&wrapper=activeConfigurations";
    final Document doc = parseUri(uri, infoMsg);

    final Collection<Job> jobs = parseJobsFromXml(doc, "activeConfiguration", false);

    // call jenkins after parsing xml
    if (!jobs.isEmpty()) {
      log.info("Fetching last completed build info for " + jobs.size() + " child jobs of " + parentJob.getName() + "...");

      for (final Iterator<Job> iter = jobs.iterator(); iter.hasNext();) {
        final Job job = iter.next();
        job.setLastCompletedBuild(getLastCompletedBuild(job));

        if (job.getLastCompletedBuild() == null) {
          // job has no last completed build nor children, ignore it
          iter.remove();
        }
      }
    }

    return jobs;
  }

  private TestReport readTestReport(URI buildUrl) throws SAXException, IOException {

    final TestReport testReport = new TestReport();

    final DefaultHandler handler = new ReadTestReportHandler(testReport);

    try {
      saxParser.parse(buildUrl + "testReport/api/xml", handler);
    }
    catch (final FileNotFoundException _ignore) {
    }

    return testReport;
  }

  private View readView(URI viewUrl) throws URISyntaxException, SAXException, IOException {
    final Document doc = builder.parse(viewUrl.toASCIIString() + "/api/xml?tree=name,url");

    final View tmpView = new View();
    tmpView.setUrl(new URI(doc.getElementsByTagName("url").item(0).getTextContent()));
    tmpView.setName(doc.getElementsByTagName("name").item(0).getTextContent());

    return tmpView;
  }

}
