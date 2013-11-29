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

import org.apache.commons.lang.StringUtils;
import org.w3c.dom.Document;
import org.w3c.dom.NodeList;

public class DevToStagingViewMapper implements ViewMapper {
  private static final String DEV_SUFFIX = "-dev";
  private static final String STAGING_SUFFIX = "-staging-dev";

  private static final String DEV_SLOW_SUFFIX = "-dev-slow";
  private static final String STAGING_SLOW_SUFFIX = "-staging-dev-slow";

  @Override
  public String mapJobName(String jobName) {
    if (jobName.endsWith(DEV_SUFFIX)) {
      return StringUtils.substringBefore(jobName, DEV_SUFFIX) + STAGING_SUFFIX;
    }
    if (jobName.endsWith(DEV_SLOW_SUFFIX)) {
      return StringUtils.substringBefore(jobName, DEV_SLOW_SUFFIX) + STAGING_SLOW_SUFFIX;
    }
    return null;
  }

  @Override
  public Document mapConfigXml(Document configXml) {
    transformGroovyScripts(configXml);
    return configXml;
  }

  private void transformGroovyScripts(Document configXml) {
    NodeList nodes = configXml.getElementsByTagName("groovyScript");
    for (int i = 0; i < nodes.getLength(); i++) {
      String text = nodes.item(i).getTextContent();
      text = text.replace(
          "[\"buildIsWorseChat\": null, \"buildIsNoBetter\": \"LR Dev Tests\"]",
          "[\"buildIsWorseChat\": \"LR-2.6-staging-tests\", \"buildIsNoBetter\": \"LR-2.6-staging-tests\"]");
      nodes.item(i).setTextContent(text);
    }
  }
}
