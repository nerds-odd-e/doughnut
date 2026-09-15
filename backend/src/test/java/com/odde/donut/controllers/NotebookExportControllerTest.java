package com.odde.donut.controllers;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.odde.donut.entities.Folder;
import com.odde.donut.entities.Notebook;
import com.odde.donut.exceptions.UnexpectedNoAccessRightException;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;

class NotebookExportControllerTest extends NotebookControllerTestBase {

  @Test
  void exportsNotebookAsAttachmentZip() throws UnexpectedNoAccessRightException, IOException {
    Notebook nb = topNote.getNotebook();
    Folder emptyFolder = ownedFolder(nb, "Ideas");
    Integer emptyFolderId = emptyFolder.getId();

    ResponseEntity<byte[]> response = controller.exportNotebook(nb);

    assertThat(response.getStatusCode(), equalTo(HttpStatus.OK));
    assertThat(
        response.getHeaders().getContentType(), equalTo(MediaType.valueOf("application/zip")));
    assertThat(
        response.getHeaders().getFirst(HttpHeaders.CONTENT_DISPOSITION),
        containsString("attachment;"));
    List<String> entryPaths = new ArrayList<>();
    try (ZipInputStream zis = new ZipInputStream(new ByteArrayInputStream(response.getBody()))) {
      ZipEntry entry;
      while ((entry = zis.getNextEntry()) != null) {
        entryPaths.add(entry.getName());
        if (entry.getName().equals("Ideas/.keep")) {
          assertThat(zis.readAllBytes().length, equalTo(0));
        }
      }
    }
    assertThat(entryPaths, contains(topNote.getTitle() + ".md", "Ideas/.keep"));
    makeMe.entityPersister.flushAndClear();
    assertThat(
        makeMe.entityPersister.find(Folder.class, emptyFolderId).getId(), equalTo(emptyFolderId));
  }

  @Test
  void deniesExportForNotebookTheCurrentUserCannotRead() {
    Notebook other = makeMe.aNotebook().please();
    assertThrows(UnexpectedNoAccessRightException.class, () -> controller.exportNotebook(other));
  }
}
