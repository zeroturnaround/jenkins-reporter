package org.zeroturnaround.jenkins.reporter;

public class DocumentNotFoundException extends RuntimeException {

  public DocumentNotFoundException(String message, Throwable cause) {
    super(message, cause);
  }

  public DocumentNotFoundException(Throwable cause) {
    super(cause);
  }

}
