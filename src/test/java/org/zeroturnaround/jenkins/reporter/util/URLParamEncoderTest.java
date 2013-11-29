package org.zeroturnaround.jenkins.reporter.util;

import static org.junit.Assert.*;

import org.junit.Test;

public class URLParamEncoderTest {

  @Test
  public void testEncode() {
    String input = "parameter with a space or more";
    String expected = "parameter%20with%20a%20space%20or%20more";
    
    String actual = URLParamEncoder.encode(input);
    
    assertEquals(expected, actual);
  }

}
