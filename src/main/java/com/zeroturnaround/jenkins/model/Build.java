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
package com.zeroturnaround.jenkins.model;

import java.net.URI;
import java.util.Date;

public class Build {
  private String duration;
  private int id;
  private String result;
  private TestReport testReport;
  private Date timestamp;
  private URI url;
  private int dayOfYear;

  public String getDuration() {
    return duration;
  }

  public int getId() {
    return id;
  }

  public String getResult() {
    return result;
  }

  public TestReport getTestReport() {
    return testReport;
  }

  public Date getTimestamp() {
    return (Date)timestamp.clone();
  }

  public URI getUrl() {
    return url;
  }

  public void setDuration(String duration) {
    this.duration = duration;
  }

  public void setId(int id) {
    this.id = id;
  }

  public void setResult(String result) {
    this.result = result;
  }

  public void setTestReport(TestReport testReport) {
    this.testReport = testReport;
  }

  public void setTimestamp(Date timestamp) {
    this.timestamp = (Date)timestamp.clone();
  }

  public void setUrl(URI url) {
    this.url = url;
  }

  public void setDayOfYear(int dayOfYear) {
    this.dayOfYear = dayOfYear;
  }

  public int getDayOfYear() {
    return dayOfYear;
  }
}