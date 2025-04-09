package com.odde.doughnut.services;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasItems;

import com.odde.doughnut.entities.LinkType;
import com.odde.doughnut.entities.Note;
import com.odde.doughnut.factoryServices.ModelFactoryService;
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
  @Autowired ModelFactoryService modelFactoryService;

  private ObsidianFormatService obsidianFormatService;
  private Note headNote;

  @BeforeEach
  void setup() {
    var user = makeMe.aUser().please();
    headNote = makeMe.aNote().please();
    obsidianFormatService = new ObsidianFormatService(user, modelFactoryService);
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

  @Test
  void shouldNotFailWhenANoteIsTaggedTwiceByTheSameTarger() throws IOException {
    // Arrange
    Note targetNote = makeMe.aNote("Tagged Note").details("This note is tagged").please();

    // Create two different notes that both tag the same target note
    Note tagger1 = makeMe.aNote("Tagger 1").under(headNote).please();

    // Create tag links from both taggers to the target
    makeMe.aReification().between(tagger1, targetNote, LinkType.TAGGED_BY).please();
    makeMe.aReification().between(tagger1, targetNote, LinkType.TAGGED_BY).please();

    makeMe.refresh(headNote.getNotebook());

    // Act & Assert
    // This should throw a ZipException for duplicate entry
    byte[] zipBytes = obsidianFormatService.exportToObsidian(headNote);

    // The test should never reach this point because of the exception
    Map<String, String> zipContents = extractZipContents(zipBytes);
    assertThat(zipContents.size(), org.hamcrest.Matchers.greaterThan(0));
  }

  @Test
  void shouldNotFailWhenSameNoteIsTaggedByDifferentTaggers() throws IOException {
    // Arrange
    Note targetNote = makeMe.aNote("Tagged Note").details("This note is tagged").please();

    // Create two different notes that both tag the same target note
    Note tagger1 = makeMe.aNote("Tagger 1").under(headNote).please();
    Note tagger2 = makeMe.aNote("Tagger 2").under(headNote).please();

    // Create tag links from both taggers to the target
    makeMe.aReification().between(tagger1, targetNote, LinkType.TAGGED_BY).please();
    makeMe.aReification().between(tagger2, targetNote, LinkType.TAGGED_BY).please();

    makeMe.refresh(headNote.getNotebook());

    // Act & Assert
    byte[] zipBytes = obsidianFormatService.exportToObsidian(headNote);

    // The test should pass now with our fix
    Map<String, String> zipContents = extractZipContents(zipBytes);
    assertThat(zipContents.size(), org.hamcrest.Matchers.greaterThan(0));
  }
}
