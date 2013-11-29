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
package org.zeroturnaround.jenkins.reporter;

import static com.google.common.collect.Lists.newArrayList;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.velocity.Template;
import org.apache.velocity.VelocityContext;
import org.apache.velocity.tools.generic.DateTool;
import org.apache.velocity.tools.generic.EscapeTool;
import org.apache.velocity.tools.generic.NumberTool;
import org.zeroturnaround.jenkins.reporter.model.JenkinsView;
import org.zeroturnaround.jenkins.reporter.model.Job;
import org.zeroturnaround.jenkins.reporter.util.CommentsHelper;

public class JenkinsReportGenerator {
  private final Template template;

  public JenkinsReportGenerator(Template template) {
    this.template = template;
  }

  public void generateReport(JenkinsView viewData, PrintWriter out) throws IOException {
    final List<Job> failedJobs = newArrayList();
    failedJobs.addAll(viewData.getFailedJobs());

    // sort jobs by build timestamp
    Collections.sort(failedJobs, new JobByTimestampComparator());

    final List<Job> passedJobs = newArrayList();
    passedJobs.addAll(viewData.getPassedJobs());

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

    context.put("view", viewData);
    context.put("failedJobs", failedJobs);
    context.put("passedJobs", passedJobs);
    context.put("commentsHelper", createCommentsHelper(viewData.getName()));

    final StringWriter writer = new StringWriter();
    template.merge(context, writer);

    out.println(writer.toString());

    out.flush();
    out.close();
  }

  // more relaxed file format - only "=" sign should be escaped in the key
  // with "\"
  private static CommentsHelper createCommentsHelper(final String jenkinsViewName) throws IOException {
    final String failureCommentsDir = "src/main/resources";
    final File file = new File(failureCommentsDir, "failureComments-" + jenkinsViewName + ".properties");
    return new CommentsHelper().load(file);
  }

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
}
