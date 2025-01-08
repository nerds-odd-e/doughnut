package com.odde.doughnut.entities;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasItems;
import static org.hamcrest.Matchers.containsString;
import static org.junit.jupiter.api.Assertions.*;

import com.odde.doughnut.testability.MakeMe;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
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
  void generateObsidianExportShouldCreateValidZipFileWithIndexFiles() throws IOException {
    // Create test notes with hierarchy
    headNote.setTopicConstructor("Root Note");
    headNote.setDetails("Root Content");
    Note note1 = makeMe.aNote("Parent Note").under(headNote).details("Parent Content").please();
    Note note2 = makeMe.aNote("Child Note").under(note1).details("Child Content").please();
    Note note3 = makeMe.aNote("Leaf Note").under(note1).details("Leaf Content").please();
    makeMe.refresh(notebook);

    // Generate export
    byte[] zipBytes = notebook.generateObsidianExport();

    // Verify zip contents
    try (ByteArrayInputStream bais = new ByteArrayInputStream(zipBytes);
        ZipInputStream zis = new ZipInputStream(bais)) {

      Map<String, String> zipContents = new HashMap<>();
      ZipEntry entry;
      while ((entry = zis.getNextEntry()) != null) {
        byte[] content = zis.readAllBytes();
        zipContents.put(entry.getName(), new String(content, StandardCharsets.UTF_8));
      }

      // Verify the structure and content
      assertThat(
          zipContents.keySet(),
          hasItems(
              "Root Note/__index.md",
              "Root Note/Parent Note/__index.md",
              "Root Note/Parent Note/Child Note.md",
              "Root Note/Parent Note/Leaf Note.md"));

      // Verify content of files
      assertThat(zipContents.get("Root Note/__index.md"), 
          containsString("# Root Note\nRoot Content"));

      assertThat(zipContents.get("Root Note/Parent Note/__index.md"),
          containsString("# Parent Note\nParent Content")); 

      assertThat(zipContents.get("Root Note/Parent Note/Child Note.md"),
          containsString("# Child Note\nChild Content"));

      assertThat(zipContents.get("Root Note/Parent Note/Leaf Note.md"), 
          containsString("# Leaf Note\nLeaf Content"));
    }
  }
}
