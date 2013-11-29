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

import static com.google.common.collect.Lists.newArrayList;

import java.net.URI;
import java.util.ArrayList;
import java.util.Collection;

import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.zeroturnaround.jenkins.model.Job.BadJobPredicate;
import com.zeroturnaround.jenkins.model.Job.GoodJobPredicate;

public class View {
  private int failCount;
  private Collection<Job> jobs = newArrayList();
  private int jobsTotal;
  private String name;
  private int testsTotal;
  private URI url;

  public int getFailCount() {
    return failCount;
  }

  public Collection<Job> getFailedJobs() {
    return Lists.newArrayList(Iterables.filter(getJobs(), new BadJobPredicate()));
  }

  public float getFailureRate() {
    if (getTestsTotal() != 0) {
      return 100 * (float) getFailCount() / getTestsTotal();
    }
    else {
      return 0;
    }
  }

  public Collection<Job> getJobs() {
    return new ArrayList<Job>(jobs);
  }

  public int getJobsTotal() {
    return jobsTotal;
  }

  public String getName() {
    return name;
  }

  public Collection<Job> getPassedJobs() {
    return Lists.newArrayList(Iterables.filter(getJobs(), new GoodJobPredicate()));
  }

  public int getTestsTotal() {
    return testsTotal;
  }

  public URI getUrl() {
    return url;
  }

  public void setFailCount(int failCount) {
    this.failCount = failCount;
  }

  public void setJobs(Collection<Job> jobs) {
    this.jobs = jobs;

    if (jobs != null) {
      for (final Job job : jobs) {
        if (job.getLastCompletedBuild() != null && job.getLastCompletedBuild().getTestReport() != null) {
          failCount += job.getLastCompletedBuild().getTestReport().getFailCount();
          testsTotal += job.getLastCompletedBuild().getTestReport().getTotalCount();
        }
      }
    }
  }

  public void setJobsTotal(int jobsTotal) {
    this.jobsTotal = jobsTotal;
  }

  public void setName(String name) {
    this.name = name;
  }

  public void setTestsTotal(int testsTotal) {
    this.testsTotal = testsTotal;
  }

  public void setUrl(URI url) {
    this.url = url;
  }
}