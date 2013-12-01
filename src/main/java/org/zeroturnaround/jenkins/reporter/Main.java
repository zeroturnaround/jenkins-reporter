package org.zeroturnaround.jenkins.reporter;

import java.awt.Desktop;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.URI;
import java.net.URISyntaxException;
import java.text.SimpleDateFormat;
import java.util.Date;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.zeroturnaround.jenkins.reporter.model.JenkinsView;
import org.zeroturnaround.jenkins.reporter.util.URLParamEncoder;

/**
 * Main entry point
 */
public class Main {
  private static final Logger log = LoggerFactory.getLogger(Main.class); // NOSONAR
  
  private static final String JENKINS_URL_PROPERTY = "reporter.jenkins.url";
  private static final String VIEW_URL_PREFIX_PROPERTY = "reporter.jenkins.view.prefix";
  private static final String REPORTER_NAME_PREFIX_PROPERTY = "reporter.name.prefix";
  private static final String REPORTER_OUTPUT_FILE_PROPERTY = "reporter.output.file";

  /**
   * The HTTP url of your Jenkins instances. For example http://jenkins/
   */
  private static final String JENKINS_URL = System.getProperty(JENKINS_URL_PROPERTY);

  /**
   * If you are using nested views then you need to set prefix for the URL for the views. By default
   * the value is /view/ so that the URL construction be like JENKINS_URL + /view/ + viewName. For
   * nested view you might need to do some like /view/NestedGroup/view/
   */
  private static final String VIEW_URL_PREFIX = System.getProperty(VIEW_URL_PREFIX_PROPERTY, "/view/");

  private static final String OUTPUT_FILE_NAME = System.getProperty(REPORTER_OUTPUT_FILE_PROPERTY);
  public static final String JOB_NAME_PREFIX = System.getProperty(REPORTER_NAME_PREFIX_PROPERTY);

  public static final void main(String[] args) {
    if (args.length == 0) {
      System.err.println("Please give the name of Jenkins view as parameter to this script."); // NOSONAR
      System.out.println();
      printUsage();
      System.exit(-1);
    }

    if (!validateArguments()) {
      System.out.println();
      printUsage();
      System.exit(1);
    }

    // lets support URL's ending with a slash and also without one
    String jenkinsUrl = JENKINS_URL;
    if (JENKINS_URL.endsWith("/"))
      jenkinsUrl = JENKINS_URL.substring(0, JENKINS_URL.length() - 1);

    // Lets generate a report for all the view specified
    for (final String jenkinsViewName : args) {
      URI viewUrl;
      try {
        String viewUrlPrefix = VIEW_URL_PREFIX;
        if (!viewUrlPrefix.startsWith("/")) {
          viewUrlPrefix = "/" + viewUrlPrefix;
        }

        if (!viewUrlPrefix.endsWith("/"))
          viewUrlPrefix = viewUrlPrefix + "/";

        viewUrl = new URI(jenkinsUrl + VIEW_URL_PREFIX + URLParamEncoder.encode(jenkinsViewName));
      }
      catch (URISyntaxException e) {
        throw new ProcessingException(e);
      }

      // lets generate a output filename if none provided
      String outputFileName = OUTPUT_FILE_NAME;
      if (outputFileName == null) {
        final SimpleDateFormat sdf = new SimpleDateFormat("'jenkins-report-'yyyy-MM-dd_HH.mm.ss");
        outputFileName = jenkinsViewName + "-" + sdf.format(new Date()) + "-";
      }
      log.debug("Using view URL {} and generating output to {}", viewUrl, outputFileName);

      File outputFile;
      PrintWriter out;
      try {
        outputFile = File.createTempFile(outputFileName, ".html");
        out = new PrintWriter(new FileWriter(outputFile));
      }
      catch (IOException e) {
        throw new ProcessingException("Unable to create file " + outputFileName, e);
      }

      // ViewData viewData =
      JenkinsViewAnalyser jHelper = (new JenkinsHelperBuilder()).createDefault();
      JenkinsView viewData = jHelper.getViewData(viewUrl);

      final JenkinsReportGenerator app = (new JenkinsReportGeneratorBuilder()).buildDefaultGenerator();
      app.generateReport(viewData, out);

      if (Desktop.isDesktopSupported()) {
        try {
          Desktop.getDesktop().open(outputFile);
        }
        catch (IOException e) {
        }
      }
      log.info("Generated report to: " + outputFile);
    }
  }

  private static void printUsage() {
    System.out.println("Program Usage");
    System.out.println();
    System.out.println("Available options:");
    System.out.println("Required -D"+JENKINS_URL_PROPERTY+"=http://your-jenkins-instance.com/");
    System.out.println();
    System.out.println("Optional -D"+VIEW_URL_PREFIX_PROPERTY+"=/view/");
    System.out.println("\tFor nested views you want to specify the address part before the view. Defaults to /view/");
    System.out.println();
    System.out.println("Optional -D"+REPORTER_NAME_PREFIX_PROPERTY+"=job-name-prefix");
    System.out.println("\tOnly include jobs with names that start with this prefix.");
    System.out.println();
    System.out.println("Optional -D"+REPORTER_OUTPUT_FILE_PROPERTY+"=output-file-name");
    System.out.println("\tSpecify output filename. By default writes to TMP folder and outputs file name");
  }

  private static boolean validateArguments() {
    if (VIEW_URL_PREFIX == null) {
      System.out.println(String.format("Please provide your Jenkins view url pattern with -D%s", VIEW_URL_PREFIX_PROPERTY)); // NOSONAR
      return false;
    }

    if (JENKINS_URL == null) {
      System.out.println(String.format("Please provide your jenkins URL via JVM property -D%s", JENKINS_URL_PROPERTY)); // NOSONAR
      return false;
    }
    return true;
  }
}
