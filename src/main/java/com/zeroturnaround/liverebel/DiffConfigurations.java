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

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringEscapeUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.velocity.Template;
import org.apache.velocity.VelocityContext;
import org.apache.velocity.app.VelocityEngine;
import org.apache.velocity.runtime.RuntimeConstants;
import org.apache.velocity.runtime.resource.loader.ClasspathResourceLoader;
import org.custommonkey.xmlunit.DetailedDiff;
import org.custommonkey.xmlunit.Diff;
import org.custommonkey.xmlunit.Difference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.Text;

import com.zeroturnaround.jenkins.model.Job;
import com.zeroturnaround.jenkins.model.View;

public class DiffConfigurations {
  public static class DiffReport {
    private final Job jobA;
    private final Job jobB;
    private final List<DiffReportItem> items;

    public DiffReport(Job jobA, Job jobB, List<DiffReportItem> items) {
      super();
      this.jobA = jobA;
      this.jobB = jobB;
      this.items = items;
    }

    public Job getJobA() {
      return jobA;
    }

    public Job getJobB() {
      return jobB;
    }

    public List<DiffReportItem> getItems() {
      return items;
    }
  }

  public static class DiffReportItem {
    private final String reason;
    private final String xmlA;
    private final String xmlB;

    public DiffReportItem(String reason, String xmlA, String xmlB) {
      this.reason = reason;
      this.xmlA = xmlA;
      this.xmlB = xmlB;
    }

    public String getReason() {
      return reason;
    }

    public String getXmlA() {
      return xmlA;
    }

    public String getXmlB() {
      return xmlB;
    }
  }

  private static final Logger log = LoggerFactory.getLogger(DiffConfigurations.class); //NOSONAR

  private static final String[] IGNORE_XPATHS = {
      "/project[1]/description[1]/text()[1]",
      "/matrix-project[1]/description[1]/text()[1]",
      "/maven2-moduleset[1]/description[1]/text()[1]",
  };
  private static final Set<String> IGNORE_XPATHS_SET = new HashSet<String>(Arrays.asList(IGNORE_XPATHS));

  private static final String[] IGNORE_REASONS = {
      "sequence of child nodes",
      "presence of child node",
  };
  private static final Set<String> IGNORE_REASONS_SET = new HashSet<String>(Arrays.asList(IGNORE_REASONS));

  public DiffConfigurations() {

  }

  public static void diffConfigurations(String[] args) throws Exception {
    if (args.length != 3) {
      System.out.println("Diff usage:"); //NOSONAR
      System.out.println("java -jar jenkins-reporter-standalone.jar diff %username% %authtoken%"); //NOSONAR
      System.exit(-1); //NOSONAR
    }

    JenkinsService service = new JenkinsService(args[1], args[2]);
    View v1 = service.readView("LR Dev");
    View v2 = service.readView("LR Dev Staging");

    DiffConfigurations diff = new DiffConfigurations();
    diff.diff(v1, v2, new DevToStagingViewMapper());
  }

  public void diff(View original, View transformed, ViewMapper mapper) throws Exception {
    VelocityContext context = new VelocityContext();

    log.info("Comparing {} and {} views.", original.getName(), transformed.getName());
    List<DiffReport> reports = new ArrayList<DiffReport>();
    Map<Job, Job> getJobMapping = getJobMapping(original, transformed, mapper, context);
    for (Map.Entry<Job, Job> entry : getJobMapping.entrySet()) {
      Job originalJob = entry.getKey();
      Job transformedJob = entry.getValue();

      log.info("Comapring {} and {} jobs.", originalJob.getName(), transformedJob.getName());

      List<DiffReportItem> reportItems = compareConfigXml(originalJob.getConfigXml(), transformedJob.getConfigXml(), mapper);
      reports.add(new DiffReport(originalJob, transformedJob, reportItems));
    }

    context.put("reports", reports);
    generateReport(context);
  }

  private List<DiffReportItem> compareConfigXml(Document oConf, Document tConf, ViewMapper mapper) {
    DetailedDiff xmlDiff = new DetailedDiff(new Diff(mapper.mapConfigXml(oConf), tConf));
    List<DiffReportItem> reportItems = new ArrayList<DiffReportItem>();

    @SuppressWarnings("unchecked")
    List<Difference> differences = xmlDiff.getAllDifferences();

    for (Difference difference : differences) {
      if (isIgnored(difference))
        continue;

      reportItems.add(new DiffReportItem(
          difference.getDescription(),
          nodeToString(difference.getControlNodeDetail().getNode()),
          nodeToString(difference.getTestNodeDetail().getNode())));
    }

    return reportItems;
  }

  private void generateReport(VelocityContext context) {
    final VelocityEngine velocityEngine = new VelocityEngine();
    velocityEngine.setProperty(RuntimeConstants.RESOURCE_LOADER, "classpath");
    velocityEngine.setProperty("classpath.resource.loader.class", ClasspathResourceLoader.class.getName());
    velocityEngine.init();

    Template template = velocityEngine.getTemplate("diff.vm");
    context.put("test", "Hello world!");

    PrintWriter writer = null;
    try {
      writer = new PrintWriter("diff.html");
      template.merge(context, writer);
    }
    catch (Exception e) {
      throw new RuntimeException(e);
    }
    finally {
      IOUtils.closeQuietly(writer);
    }
  }

  private boolean isIgnored(Difference difference) {
    if (IGNORE_XPATHS_SET.contains(difference.getControlNodeDetail().getXpathLocation())
        || IGNORE_XPATHS_SET.contains(difference.getTestNodeDetail().getXpathLocation())) {
      return true;
    }
    else if (IGNORE_REASONS_SET.contains(difference.getDescription())) {
      return true;
    }

    Node a = difference.getControlNodeDetail().getNode();
    Node b = difference.getTestNodeDetail().getNode();
    if (isEqualTextNodes(a, b)) {
      return true;
    }

    return false;
  }

  private boolean isEqualTextNodes(Node a, Node b) {
    if (!(a instanceof Text))
      return false;
    
    if (!(b instanceof Text))
      return false;

    Text textA = (Text) a;
    Text textB = (Text) b;
    return StringUtils.equals(textA.getNodeValue().trim(), textB.getNodeValue().trim());
  }

  private Map<Job, Job> getJobMapping(View original, View transformed, ViewMapper mapper, VelocityContext context) {
    Map<Job, Job> jobMapping = new HashMap<Job, Job>();
    Map<String, Job> originalJobs = getJobsMap(original.getJobs());
    Map<String, Job> transformedJobs = getJobsMap(transformed.getJobs());

    Set<String> unmatchedJobs = new HashSet<String>();

    for (Map.Entry<String, Job> entry : originalJobs.entrySet()) {
      String name = entry.getKey();
      String mappedName = mapper.mapJobName(name);
      if (mappedName == null) {
        log.warn("Job {} in {} cannot be mapped to correspondending job in {} view.", name, original.getName(), transformed.getName());
        unmatchedJobs.add(name);
      }
      else {
        Job mappedJob = transformedJobs.get(mappedName);
        if (mappedJob == null) {
          log.warn("Job {} which should be mapped to {} is missing in {} view.", mappedName, name, transformed.getName());
          unmatchedJobs.add(name);
        }
        else {
          jobMapping.put(entry.getValue(), mappedJob);
          transformedJobs.remove(mappedName);
        }
      }
    }

    log.warn("These jobs in {} aren't mapped: {}.", StringUtils.join(transformedJobs.keySet(), ", "));

    context.put("unmappedA", unmatchedJobs);
    context.put("unmappedB", transformedJobs.keySet());

    return jobMapping;
  }

  private Map<String, Job> getJobsMap(Collection<Job> jobs) {
    Map<String, Job> jobMap = new HashMap<String, Job>(jobs.size());
    for (Job job : jobs) {
      jobMap.put(job.getName(), job);
    }
    return jobMap;
  }

  private static String nodeToString(Node node) {
    StringWriter sw = new StringWriter();
    try {
      Transformer t = TransformerFactory.newInstance().newTransformer();
      t.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "yes");
      t.setOutputProperty(OutputKeys.INDENT, "yes");
      t.transform(new DOMSource(node), new StreamResult(sw));
    }
    catch (TransformerException te) {
      System.out.println("nodeToString Transformer Exception");
    }
    return StringEscapeUtils.escapeHtml(sw.toString());
  }
};
