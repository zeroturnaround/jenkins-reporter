package org.zeroturnaround.jenkins.reporter;

public class ProcessingException extends RuntimeException {

  public ProcessingException(String message, Throwable cause) {
    super(message, cause);
  }

  public ProcessingException(Throwable cause) {
    super(cause);
  }

}
