package com.zeroturnaround.liverebel;

import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

import com.zeroturnaround.jenkins.model.TestCase;
import com.zeroturnaround.jenkins.model.TestReport;

final class ReadTestReportHandler extends DefaultHandler {
  private final TestReport testReport;
  boolean ageNode = false;
  boolean classNameNode = false;
  StringBuffer errorDetails, errorStackTrace;
  boolean errorDetailsNode = false;
  boolean errorStackTraceNode = false;
  boolean failCountNode = false;
  private boolean matrixJob;
  boolean methodNameNode = false;
  boolean passCountNode = false;
  private boolean plainJob;
  boolean rootNode = false;
  boolean skipCountNode = false;
  boolean statusNode = false;
  TestCase testCase;
  boolean totalCountNode = false;

  ReadTestReportHandler(TestReport testReport) {
    this.testReport = testReport;
  }

  @Override
  public void characters(char ch[], int start, int length) throws SAXException {
    if (rootNode && failCountNode) {
      testReport.setFailCount(Integer.parseInt(new String(ch, start, length)));
    }
    else if (rootNode && skipCountNode) {
      testReport.setSkipCount(Integer.parseInt(new String(ch, start, length)));
    }
    else if (rootNode && passCountNode) {
      testReport.setPassCount(Integer.parseInt(new String(ch, start, length)));
    }
    else if (rootNode && totalCountNode) {
      testReport.setTotalCount(Integer.parseInt(new String(ch, start, length)));
    }
    else if (statusNode) {
      testCase.setStatus(new String(ch, start, length));
    }
    else if (ageNode) {
      testCase.setAge(Integer.parseInt(new String(ch, start, length)));
    }
    else if (classNameNode) {
      testCase.setClassName(new String(ch, start, length));
    }
    else if (methodNameNode) {
      testCase.setMethodName(new String(ch, start, length));
    }
    else if (errorDetailsNode) {
      errorDetails.append(new String(ch, start, length));
    }
    else if (errorStackTraceNode) {
      errorStackTrace.append(new String(ch, start, length));
    }
  }

  @Override
  public void endElement(String uri, String localName, String qName) throws SAXException {
    if (qName.equals("matrixTestResult")) {
      rootNode = false;
    }
    else if (qName.equals("testResult")) {
      rootNode = false;
    }
    else if (qName.equalsIgnoreCase("case")) {
      if (testCase.getStatus() != null && (testCase.getStatus().equals("FAILED") || testCase.getStatus().equals("REGRESSION"))) {
        testReport.getTestCases().add(testCase);
        testCase = new TestCase(); // to avoid mutating it later (e.g. <name> tag occurs outside of <case> too)
      }
    }
    else if (failCountNode) {
      failCountNode = false;
    }
    else if (qName.equalsIgnoreCase("skipCount")) {
      skipCountNode = false;
    }
    else if (qName.equalsIgnoreCase("passCount")) {
      passCountNode = false;
    }
    else if (qName.equalsIgnoreCase("totalCount")) {
      totalCountNode = false;
    }
    else if (statusNode) {
      statusNode = false;
    }
    else if (ageNode) {
      ageNode = false;
    }
    else if (classNameNode) {
      classNameNode = false;
    }
    else if (methodNameNode) {
      methodNameNode = false;
    }
    else if (qName.equalsIgnoreCase("errorDetails")) {
      errorDetailsNode = false;
      testCase.setErrorDetails(errorDetails.toString());
      errorDetails = null;
    }
    else if (qName.equalsIgnoreCase("errorStackTrace")) {
      errorStackTraceNode = false;
      testCase.setErrorStackTrace(errorStackTrace.toString());
      errorStackTrace = null;
    }
  }

  @Override
  public void startElement(String uri, String localName, String qName, Attributes attributes) throws SAXException {
    if (qName.equals("matrixTestResult")) {
      matrixJob = true;
      rootNode = true;
    }
    else if (qName.equals("testResult")) {
      plainJob = true;
      rootNode = true;
    }
    else if (qName.equalsIgnoreCase("childReport")) {
      if (matrixJob) {
        // "childReport" is next element after the header in
        // matrix job
        rootNode = false;
      }
    }
    else if (qName.equalsIgnoreCase("suite")) {
      if (plainJob) {
        // "suite" is next element after the header in
        // plain job
        rootNode = false;

        // in case of plain job we need to calculate totalCount
        // manually
        if (testReport.getTotalCount() == 0) {
          testReport.setTotalCount(testReport.getPassCount() + testReport.getFailCount() + testReport.getSkipCount());
        }
      }
    }
    else if (qName.equalsIgnoreCase("case")) {
      testCase = new TestCase();
    }
    else if (qName.equalsIgnoreCase("failCount")) {
      failCountNode = true;
    }
    else if (qName.equalsIgnoreCase("skipCount")) {
      skipCountNode = true;
    }
    else if (qName.equalsIgnoreCase("passCount")) {
      passCountNode = true;
    }
    else if (qName.equalsIgnoreCase("totalCount")) {
      totalCountNode = true;
    }
    else if (qName.equalsIgnoreCase("status")) {
      statusNode = true;
    }
    else if (qName.equalsIgnoreCase("age")) {
      ageNode = true;
    }
    else if (qName.equalsIgnoreCase("className")) {
      classNameNode = true;
    }
    else if (qName.equalsIgnoreCase("name")) {
      methodNameNode = true;
    }
    else if (qName.equalsIgnoreCase("errorDetails")) {
      errorDetailsNode = true;
      errorDetails = new StringBuffer();
    }
    else if (qName.equalsIgnoreCase("errorStackTrace")) {
      errorStackTraceNode = true;
      errorStackTrace = new StringBuffer();
    }

  }
}