package com.odde.doughnut.services;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasItems;

import com.odde.doughnut.entities.Note;
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
class ObsidianFormatServiceTest {
  @Autowired private MakeMe makeMe;
  @Autowired private ObsidianFormatService obsidianFormatService;
  private Note headNote;

  @BeforeEach
  void setup() {
    headNote = makeMe.aNote().please();
  }

  @Test
  void shouldGenerateValidZipFileWithCorrectStructureAndContent() throws IOException {
    // Arrange
    headNote.setTopicConstructor("Root Note");
    headNote.setDetails("Root Content");
    Note note1 = makeMe.aNote("Parent Note").under(headNote).details("Parent Content").please();
    Note note2 = makeMe.aNote("Child Note").under(note1).details("Child Content").please();
    Note note3 = makeMe.aNote("Leaf Note").under(note1).details("Leaf Content").please();
    makeMe.refresh(headNote.getNotebook());

    // Act
    byte[] zipBytes = obsidianFormatService.exportToObsidian(headNote);

    // Assert
    Map<String, String> zipContents = extractZipContents(zipBytes);
    verifyZipStructure(zipContents);
    verifyZipContents(zipContents);
  }

  private Map<String, String> extractZipContents(byte[] zipBytes) throws IOException {
    Map<String, String> zipContents = new HashMap<>();
    try (ByteArrayInputStream bais = new ByteArrayInputStream(zipBytes);
        ZipInputStream zis = new ZipInputStream(bais)) {
      ZipEntry entry;
      while ((entry = zis.getNextEntry()) != null) {
        byte[] content = zis.readAllBytes();
        zipContents.put(entry.getName(), new String(content, StandardCharsets.UTF_8));
      }
    }
    return zipContents;
  }

  private void verifyZipStructure(Map<String, String> zipContents) {
    assertThat(
        zipContents.keySet(),
        hasItems(
            "Root Note/__index.md",
            "Root Note/Parent Note/__index.md",
            "Root Note/Parent Note/Child Note.md",
            "Root Note/Parent Note/Leaf Note.md"));
  }

  private void verifyZipContents(Map<String, String> zipContents) {
    assertThat(
        zipContents.get("Root Note/__index.md"), containsString("# Root Note\nRoot Content"));

    assertThat(
        zipContents.get("Root Note/Parent Note/__index.md"),
        containsString("# Parent Note\nParent Content"));

    assertThat(
        zipContents.get("Root Note/Parent Note/Child Note.md"),
        containsString("# Child Note\nChild Content"));

    assertThat(
        zipContents.get("Root Note/Parent Note/Leaf Note.md"),
        containsString("# Leaf Note\nLeaf Content"));
  }
}
