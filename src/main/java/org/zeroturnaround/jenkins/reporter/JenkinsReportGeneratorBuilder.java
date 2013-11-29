package org.zeroturnaround.jenkins.reporter;

import org.apache.velocity.Template;
import org.apache.velocity.app.VelocityEngine;
import org.apache.velocity.runtime.RuntimeConstants;
import org.apache.velocity.runtime.resource.loader.ClasspathResourceLoader;

public class JenkinsReportGeneratorBuilder {
  public JenkinsReportGenerator buildDefaultGenerator() {
    final VelocityEngine velocityEngine = new VelocityEngine();
    velocityEngine.setProperty(RuntimeConstants.RESOURCE_LOADER, "classpath");
    velocityEngine.setProperty("classpath.resource.loader.class", ClasspathResourceLoader.class.getName());
    velocityEngine.init();

    Template template = velocityEngine.getTemplate("report.vm");

    return new JenkinsReportGenerator(template);
  }
}
