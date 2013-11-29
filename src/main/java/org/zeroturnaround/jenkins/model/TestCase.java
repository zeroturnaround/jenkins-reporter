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

public class TestCase {
  private Integer age;
  private String className;
  private String errorDetails;
  private String errorStackTrace;
  private String methodName, status;

  public Integer getAge() {
    return age;
  }

  public String getClassName() {
    return className;
  }

  public String getErrorDetails() {
    return errorDetails;
  }

  public String getErrorStackTrace() {
    return errorStackTrace;
  }

  public String getMethodName() {
    return methodName;
  }

  public String getStatus() {
    return status;
  }

  public void setAge(Integer age) {
    this.age = age;
  }

  public void setClassName(String className) {
    this.className = className;
  }

  public void setErrorDetails(String errorDetails) {
    this.errorDetails = errorDetails;
  }

  public void setErrorStackTrace(String errorStackTrace) {
    this.errorStackTrace = errorStackTrace;
  }

  public void setMethodName(String methodName) {
    this.methodName = methodName;
  }

  public void setStatus(String status) {
    this.status = status;
  }
}