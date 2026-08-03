package com.odde.doughnut.controllers;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.odde.doughnut.entities.Notebook;
import com.odde.doughnut.exceptions.UnexpectedNoAccessRightException;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.zip.ZipInputStream;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockHttpServletRequest;

class NotebookExportControllerTest extends NotebookControllerTestBase {

  private MockHttpServletRequest exportRequest() {
    MockHttpServletRequest request = new MockHttpServletRequest();
    request.setScheme("http");
    request.setServerName("localhost");
    request.setServerPort(9081);
    return request;
  }

  @Test
  void exportsNotebookAsAttachmentZip() throws UnexpectedNoAccessRightException, IOException {
    Notebook nb = topNote.getNotebook();

    ResponseEntity<byte[]> response = controller.exportNotebook(exportRequest(), nb);

    assertThat(response.getStatusCode(), equalTo(HttpStatus.OK));
    assertThat(
        response.getHeaders().getContentType(), equalTo(MediaType.valueOf("application/zip")));
    assertThat(
        response.getHeaders().getFirst(HttpHeaders.CONTENT_DISPOSITION),
        containsString("attachment;"));
    try (ZipInputStream zis = new ZipInputStream(new ByteArrayInputStream(response.getBody()))) {
      assertThat(zis.getNextEntry().getName(), equalTo(topNote.getTitle() + ".md"));
    }
  }

  @Test
  void deniesExportForNotebookTheCurrentUserCannotRead() {
    Notebook other = makeMe.aNotebook().please();
    assertThrows(
        UnexpectedNoAccessRightException.class,
        () -> controller.exportNotebook(exportRequest(), other));
  }
}
