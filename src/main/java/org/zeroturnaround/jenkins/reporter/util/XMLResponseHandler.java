package org.zeroturnaround.jenkins.reporter.util;

import java.io.IOException;
import java.io.InputStream;

import javax.xml.parsers.DocumentBuilder;

import org.apache.commons.io.IOUtils;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpResponseException;
import org.apache.http.client.ResponseHandler;
import org.w3c.dom.Document;
import org.xml.sax.SAXException;

public class XMLResponseHandler implements ResponseHandler<Document>{
  private final DocumentBuilder builder;

  public XMLResponseHandler(DocumentBuilder builder) {
    this.builder = builder;
  }

  @Override
  public Document handleResponse(HttpResponse response) throws ClientProtocolException, IOException {
    int status = response.getStatusLine().getStatusCode();
    if (status != HttpStatus.SC_OK) {
      throw new HttpResponseException(status, "200 status code expected but was " + status);
    }

    HttpEntity entity = response.getEntity();
    if (entity == null) {
      throw new ClientProtocolException("Response entity is null");
    }

    InputStream stream = entity.getContent();
    try {
      return builder.parse(entity.getContent());
    }
    catch (IllegalStateException e) {
      throw new ClientProtocolException(e);
    }
    catch (SAXException e) {
      throw new ClientProtocolException(e);
    }
    finally {
      IOUtils.closeQuietly(stream);
    }
  }

}
