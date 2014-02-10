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
package org.zeroturnaround.jenkins.reporter.model;

import java.net.URI;
import java.util.Collection;
import java.util.concurrent.atomic.AtomicInteger;

import org.w3c.dom.Document;

import com.google.common.base.Predicate;

public class Job {
  public static class BadJobPredicate implements Predicate<Job>{
    @Override
    public boolean apply(Job job) {
      return !(job.getFailCount() == 0 && job.isGreen());
    }
  }

  public static class GoodJobPredicate implements Predicate<Job>{
    @Override
    public boolean apply(Job job) {
      return job.getFailCount() == 0 && job.isGreen();
    }
  }

  private static AtomicInteger counter = new AtomicInteger(0);
  private Collection<Job> children;
  private String color;
  private final int id;
  private Build lastCompletedBuild;
  private String name;
  private URI url;
  private Document configXml;

  public Document getConfigXml() {
    return configXml;
  }

  public void setConfigXml(Document configXml) {
    this.configXml = configXml;
  }

  public Job() {
    this.id = counter.incrementAndGet();
  }

  public Collection<Job> getChildren() {
    return children;
  }

  public boolean isRed() {
    return "red".equals(color) || "red_anime".equals(color);
  }

  public boolean isGreen() {
    return "blue".equals(color) || "blue_anime".equals(color);
  }

  public boolean isGrey() {
    return "aborted".equals(color) || "aborted".equals(color);
  }

  public boolean isYellow() {
    return "yellow".equals(color) || "yellow_anime".equals(color);
  }


  public int getFailCount() {
    if (isMatrix()) {
      int failedCount = 0;
      for (Job child : getChildren()) {
        failedCount += child.getFailCount();
      }
      return failedCount;
    }
    else {
      TestReport report = getLastReport();
      return report == null ? 0 : report.getFailCount();
    }
  }

  public int getPassCount() {
    if (isMatrix()) {
      int passCount = 0;
      for (Job child : getChildren()) {
        passCount += child.getPassCount();
      }
      return passCount;
    }
    else {
      TestReport report = getLastReport();
      return report == null ? 0 : report.getTotalCount();
    }
  }

  public int getTotalCount() {
    if (isMatrix()) {
      int totalCount = 0;
      for (Job child : getChildren()) {
        totalCount += child.getTotalCount();
      }
      return totalCount;
    }
    else {
      TestReport report = getLastReport();
      return report == null ? 0 : report.getTotalCount();
    }
  }

  public double getFailureRate() {
    return (double)getFailCount()/getTotalCount();
  }

  public String getColor() {
    return color;
  }

  public int getId() {
    return id;
  }

  public Build getLastCompletedBuild() {
    return lastCompletedBuild;
  }

  public String getName() {
    return name;
  }

  public URI getUrl() {
    return url;
  }

  public boolean isMatrix() {
    return children != null && !children.isEmpty();
  }

  public void setChildren(Collection<Job> children) {
    this.children = children;
  }

  public void setColor(String color) {
    this.color = color;
  }

  public void setLastCompletedBuild(Build lastCompletedBuild) {
    this.lastCompletedBuild = lastCompletedBuild;
  }

  public void setName(String name) {
    this.name = name;
  }

  public void setUrl(URI url) {
    this.url = url;
  }

  private TestReport getLastReport() {
    Build build = getLastCompletedBuild();
    if (build == null)
      return null;

    return build.getTestReport();
  }
}