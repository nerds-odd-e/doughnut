package com.odde.doughnut.services;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasItems;
import static org.hamcrest.Matchers.is;
import static org.mockito.Mockito.when;

import com.odde.doughnut.entities.Note;
import com.odde.doughnut.entities.Notebook;
import com.odde.doughnut.entities.RelationType;
import com.odde.doughnut.entities.User;
import com.odde.doughnut.testability.MakeMe;
import com.odde.doughnut.testability.TestabilitySettings;
import com.openai.client.OpenAIClient;
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
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
@DirtiesContext
class ObsidianFormatServiceTest {
  @Autowired private MakeMe makeMe;
  @Autowired ObsidianFormatService obsidianFormatService;
  @Autowired TestabilitySettings testabilitySettings;
  @MockitoBean AuthorizationService authorizationService;

  @MockitoBean(name = "officialOpenAiClient")
  OpenAIClient officialClient;

  private Note rootNote;
  private User user;

  @BeforeEach
  void setup() {
    user = makeMe.aUser().please();
    rootNote = makeMe.aNote().please();
    when(authorizationService.getCurrentUser()).thenReturn(user);
  }

  @Test
  void shouldGenerateValidZipFileWithCorrectStructureAndContent() throws IOException {
    // Arrange
    rootNote.setTitle("Root Note");
    rootNote.setDetails("Root Content");
    Note note1 = makeMe.aNote("Parent Note").under(rootNote).details("Parent Content").please();
    Note note2 = makeMe.aNote("Child Note").under(note1).details("Child Content").please();
    Note note3 = makeMe.aNote("Leaf Note").under(note1).details("Leaf Content").please();
    Notebook notebook = rootNote.getNotebook();
    makeMe.refresh(notebook);

    // Act
    byte[] zipBytes = obsidianFormatService.exportToObsidian(notebook);

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
    Note tagger1 = makeMe.aNote("Tagger 1").under(rootNote).please();

    // Create tag relationships from both taggers to the target
    makeMe.aRelation().between(tagger1, targetNote, RelationType.TAGGED_BY).please();
    makeMe.aRelation().between(tagger1, targetNote, RelationType.TAGGED_BY).please();

    Notebook notebook = rootNote.getNotebook();
    makeMe.refresh(notebook);

    byte[] zipBytes = obsidianFormatService.exportToObsidian(notebook);

    Map<String, String> zipContents = extractZipContents(zipBytes);
    assertThat(zipContents.size(), org.hamcrest.Matchers.greaterThan(0));
  }

  @Test
  void whenOneNoteIsTaggedByTwoDifferentNotes() throws IOException {
    // Arrange
    Note targetNote = makeMe.aNote("Tagged Note").details("This note is tagged").please();

    // Create two different notes that both tag the same target note
    Note childWithTags = makeMe.aNote("Tagger 1").under(rootNote).please();

    // Create tag relationships from both taggers to the target
    makeMe.aRelation().between(childWithTags, targetNote, RelationType.TAGGED_BY).please();
    makeMe.aRelation().between(childWithTags, targetNote, RelationType.TAGGED_BY).please();

    Notebook notebook = rootNote.getNotebook();
    makeMe.refresh(notebook);

    byte[] zipBytes = obsidianFormatService.exportToObsidian(notebook);

    Map<String, String> zipContents = extractZipContents(zipBytes);
    assertThat(zipContents.size(), org.hamcrest.Matchers.greaterThan(0));

    // Verify the file names in the zip content - using the actual title of the root note
    String rootNoteTitle = rootNote.getTitle();
    assertThat(
        zipContents.keySet(),
        hasItems(
            rootNoteTitle + "/__index.md",
            rootNoteTitle + "/Tagger 1/__index.md",
            rootNoteTitle + "/Tagger 1/_tagged by.md"));
  }

  @Test
  void export_withRootIndexNote_emitsIndexMdAtRoot() throws IOException {
    makeMe.theNote(rootNote).title("Landing").slug("index").details("Index body").please();
    makeMe.aNote("Parent Note").under(rootNote).details("Parent Content").please();
    Notebook notebook = rootNote.getNotebook();
    makeMe.refresh(notebook);

    byte[] zipBytes = obsidianFormatService.exportToObsidian(notebook);
    Map<String, String> zipContents = extractZipContents(zipBytes);

    assertThat(zipContents.containsKey("index.md"), is(true));
    assertThat(zipContents.get("index.md"), containsString("Index body"));
    assertThat(zipContents.keySet(), hasItems("index.md", "Parent Note.md"));
    assertThat(zipContents.get("Parent Note.md"), containsString("# Parent Note\nParent Content"));
  }

  @Test
  void export_withoutIndexNote_omitsIndexMd() throws IOException {
    rootNote.setTitle("Root Note");
    rootNote.setDetails("Root Content");
    makeMe.aNote("Solo").under(rootNote).please();
    Notebook notebook = rootNote.getNotebook();
    makeMe.refresh(notebook);

    byte[] zipBytes = obsidianFormatService.exportToObsidian(notebook);
    Map<String, String> zipContents = extractZipContents(zipBytes);

    assertThat(zipContents.containsKey("index.md"), is(false));
    assertThat(zipContents.keySet(), hasItems("Root Note/__index.md"));
  }
}
