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

import static com.google.common.collect.Lists.newArrayList;

import java.util.Collection;

public class TestReport {
  private int failCount;
  private int passCount;
  private int skipCount;
  private Collection<TestCase> testCases = newArrayList();
  private int totalCount;

  public int getFailCount() {
    return failCount;
  }

  public float getFailureRate() {
    if (getTotalCount() != 0) {
      return 100 * (float) getFailCount() / (float) getTotalCount();
    }
    else {
      return 0;
    }
  }

  public int getPassCount() {
    return passCount;
  }

  public int getSkipCount() {
    return skipCount;
  }

  public int getSuccessRate() {
    if (getPassCount() != 0) {
      return 100 * getPassCount() / getTotalCount();
    }
    else {
      return 0;
    }
  }

  public Collection<TestCase> getTestCases() {
    return testCases;
  }

  public int getTotalCount() {
    return totalCount;
  }

  public void setFailCount(int failCount) {
    this.failCount = failCount;
  }

  public void setPassCount(int passCount) {
    this.passCount = passCount;
  }

  public void setSkipCount(int skipCount) {
    this.skipCount = skipCount;
  }

  public void setTestCases(Collection<TestCase> testCases) {
    this.testCases = testCases;
  }

  public void setTotalCount(int totalCount) {
    this.totalCount = totalCount;
  }
}