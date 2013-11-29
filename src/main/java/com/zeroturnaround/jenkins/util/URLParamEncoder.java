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
package com.zeroturnaround.jenkins.util;

public class URLParamEncoder {

  public static String encode(String input) {
    final StringBuilder resultStr = new StringBuilder();
    for (final char ch : input.toCharArray()) {
      if (isUnsafe(ch)) {
        resultStr.append('%');
        resultStr.append(toHex(ch / 16));
        resultStr.append(toHex(ch % 16));
      }
      else {
        resultStr.append(ch);
      }
    }
    return resultStr.toString();
  }

  private static boolean isUnsafe(char ch) {
    if (ch > 128 || ch < 0) {
      return true;
    }
    return " %$&+,/:;=?@<>#%".indexOf(ch) >= 0;
  }

  private static char toHex(int ch) {
    return (char) (ch < 10 ? '0' + ch : 'A' + ch - 10);
  }

}