package com.odde.doughnut.entities;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.junit.jupiter.api.Assertions.*;

import com.odde.doughnut.testability.MakeMe;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class NotebookTest {
  @Autowired MakeMe makeMe;
  Note headNote;
  Notebook notebook;

  @BeforeEach
  void setup() {
    headNote = makeMe.aNote().please();
    notebook = headNote.getNotebook();
  }

  @Test
  void aNotebookWithHeadNoteAndAChild() {
    makeMe.aNote().under(headNote).please();
    makeMe.refresh(notebook);
    assertEquals(2, notebook.getNotes().size());
  }

  @Test
  void aNotebookWithHeadNoteAndADeletedChild() {
    makeMe.aNote().under(headNote).softDeleted().please();
    makeMe.refresh(notebook);
    assertEquals(1, notebook.getNotes().size());
  }

  @Test
  void creatorId() {
    assertThat(notebook.getCreatorId())
        .isEqualTo(notebook.getCreatorEntity().getExternalIdentifier());
  }

  @Test
  void generateObsidianExportShouldCreateValidZipFile() throws IOException {
    // Create test notes
    Note note1 = makeMe.aNote("Test Note 1").under(headNote).details("Content 1").please();
    Note note2 = makeMe.aNote("Test Note 2").under(headNote).details("Content 2").please();
    makeMe.refresh(notebook);

    // Generate export
    byte[] zipBytes = notebook.generateObsidianExport();

    // Verify zip contents
    try (ByteArrayInputStream bais = new ByteArrayInputStream(zipBytes);
        ZipInputStream zis = new ZipInputStream(bais)) {

      ZipEntry entry;
      int fileCount = 0;
      while ((entry = zis.getNextEntry()) != null) {
        fileCount++;
        String content = new String(zis.readAllBytes(), StandardCharsets.UTF_8);
        assertTrue(content.startsWith("# "));
        if (entry.getName().contains("Test Note")) {
          assertTrue(content.contains("Content"));
        }
      }
      assertEquals(3, fileCount); // Head note + 2 child notes
    }
  }
}
