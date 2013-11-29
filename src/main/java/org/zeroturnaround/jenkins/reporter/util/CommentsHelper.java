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
package org.zeroturnaround.jenkins.reporter.util;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.StringUtils;
import org.zeroturnaround.jenkins.reporter.model.TestCase;

import com.google.common.base.Splitter;

/**
 * Helper class to add comments (from file loaded using {@link #load(File)}) to generated test report
 * 
 * @author Ats Uiboupin
 */
public class CommentsHelper {

  private final Map<String, String> comments = new HashMap<String, String>();

  public String getComment(TestCase testCase) {
    String className = testCase.getClassName();
    String key = className + "." + testCase.getMethodName();
    String comment = comments.get(key.trim());
    if (comment == null) {
      comment = comments.get(className);
    }
    return comment;
  }

  private static final Splitter SPLITTER_WORDS = Splitter.on(" ").trimResults().omitEmptyStrings();

  // could optimize to do the same task with state machine, but comments shouldn't be long enough to care about them
  public String wrapIssueNumberWithLink(String s) {
    String urlPrefix = "https://zeroturnaround.atlassian.net/browse/";
    if (s == null) {
      return null;
    }
    StringBuilder sb = new StringBuilder();
    for (String word : SPLITTER_WORDS.split(s)) {
      String issueCandidate = word;
      String suffix = "";
      if (issueCandidate.endsWith(".") || issueCandidate.endsWith(",") || issueCandidate.endsWith("?") || issueCandidate.endsWith("!") || issueCandidate.endsWith(";")) {
        issueCandidate = issueCandidate.substring(0, word.length() - 1);
        suffix += word.charAt(word.length() - 1);
      }
      if (issueCandidate.matches("LR-\\d+")) {
        word = "<a href='" + urlPrefix + issueCandidate + "' target='_blank'>" + issueCandidate + "<a>" + suffix;
      }
      sb.append(word).append(" ");
    }
    return sb.toString();
  }

  public CommentsHelper load(File file) throws IOException {
    if (file.isFile()) {
      List<String> fileLines = FileUtils.readLines(file);
      for (String line : fileLines) {
        line = line.trim();
        if (line.startsWith("#") || line.isEmpty()) {
          continue;
        }
        int keyIndex = getKeyIndex(line);
        String key = StringUtils.replace(line.substring(0, keyIndex).trim(), "\\=", "=");
        String comment = line.substring(keyIndex + 1);
        comments.put(key, comment);
      }
    }
    return this;
  }

  private static int getKeyIndex(String line) {
    char[] charArray = line.toCharArray();
    boolean escaping = false;
    for (int i = 0; i < charArray.length; i++) {
      char c = charArray[i];
      if (c == '=' && !escaping) {
        return i;
      }
      if (escaping) {
        escaping = false;
      }
      if (c == '\\') {
        escaping = true;
      }
    }
    throw new IllegalArgumentException("Didn't find '=' from line (or it was escaped with '\\')");
  }

}
