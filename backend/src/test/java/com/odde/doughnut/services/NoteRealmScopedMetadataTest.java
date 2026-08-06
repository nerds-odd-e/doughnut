package com.odde.doughnut.services;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.nullValue;

import com.odde.doughnut.controllers.dto.NoteRealm;
import com.odde.doughnut.entities.Folder;
import com.odde.doughnut.entities.Note;
import com.odde.doughnut.entities.Notebook;
import com.odde.doughnut.entities.User;
import com.odde.doughnut.testability.MakeMe;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class NoteRealmScopedMetadataTest {

  @Autowired MakeMe makeMe;
  @Autowired NoteRealmService noteRealmService;

  User user;
  Notebook notebook;

  @BeforeEach
  void defaultNotebook() {
    user = makeMe.aUser().please();
    notebook = makeMe.aNotebook().creatorAndOwner(user).please();
  }

  @Test
  void ancestor_folders_ordered_outermost_to_innermost() {
    Folder outer = makeMe.aFolder().notebook(notebook).name("Outer").please();
    Folder inner = makeMe.aFolder().parentFolder(outer).name("Inner").please();
    Note inFolder = makeMe.aNote().folder(inner).please();

    NoteRealm realm = noteRealmService.build(inFolder, user);

    assertThat(realm.getAncestorFolders(), hasSize(2));
    assertThat(realm.getAncestorFolders().get(0).getName(), equalTo("Outer"));
    assertThat(realm.getAncestorFolders().get(1).getName(), equalTo("Inner"));
    assertThat(realm.getAncestorFolders().get(0).getId(), equalTo(outer.getId()));
    assertThat(realm.getAncestorFolders().get(1).getId(), equalTo(inner.getId()));
  }

  @Test
  void ancestor_folders_empty_when_note_not_in_folder() {
    Note root = makeMe.aNote().notebook(notebook).please();
    assertThat(noteRealmService.build(root, user).getAncestorFolders(), empty());
  }

  @Test
  void notebook_index_applies_title_pattern_and_question_instruction_to_sibling_notes() {
    String readmeContent =
        "---\ntitle_pattern: \"{{date}}\"\nquestion_generation_instruction: Focus on definitions\n---\n";
    makeMe.theNotebook(notebook).readmeContent(readmeContent).please();
    Note normal = makeMe.aNote().notebook(notebook).please();

    NoteRealm realm = noteRealmService.build(normal, user);

    assertThat(realm.getScopedReadmeContent(), equalTo(readmeContent));
    List<String> blocks = noteRealmService.questionGenerationInstructionBlocks(normal);
    assertThat(blocks, hasSize(1));
    assertThat(blocks.get(0), containsString("Instruction from notebook"));
    assertThat(blocks.get(0), containsString("Focus on definitions"));
  }

  @ParameterizedTest
  @CsvSource({
    "title_pattern, {{date}}",
    "titlePattern, {{date}}",
  })
  void scoped_readme_content_recognizes_title_pattern_key_aliases(String key, String value) {
    String readmeContent = "---\n" + key + ": \"" + value + "\"\n---\n";
    makeMe.theNotebook(notebook).readmeContent(readmeContent).please();
    Note normal = makeMe.aNote().notebook(notebook).please();

    assertThat(
        noteRealmService.build(normal, user).getScopedReadmeContent(), equalTo(readmeContent));
  }

  @ParameterizedTest
  @CsvSource({
    "question_generation_instruction, Focus on definitions",
    "questionGenerationInstruction, Legacy key text",
  })
  void question_instruction_recognizes_instruction_key_aliases(String key, String text) {
    makeMe
        .theNotebook(notebook)
        .readmeContent("---\n" + key + ": \"" + text + "\"\n---\n")
        .please();
    Note normal = makeMe.aNote().notebook(notebook).please();

    List<String> blocks = noteRealmService.questionGenerationInstructionBlocks(normal);
    assertThat(blocks.get(0), containsString(text));
  }

  @Test
  void question_instruction_includes_every_level_ordered_notebook_then_folders_no_override() {
    makeMe
        .theNotebook(notebook)
        .readmeContent(
            "---\ntitle_pattern: \"nb\"\nquestion_generation_instruction: nb-text\n---\n")
        .please();

    Folder outer = makeMe.aFolder().notebook(notebook).name("Outer").please();
    makeMe
        .theFolder(outer)
        .readmeContent(
            "---\ntitle_pattern: \"outer\"\nquestion_generation_instruction: outer-text\n---\n")
        .please();

    Folder inner = makeMe.aFolder().parentFolder(outer).name("Inner").please();
    String innerReadme =
        "---\ntitle_pattern: \"inner\"\nquestion_generation_instruction: inner-text\n---\n";
    makeMe.theFolder(inner).readmeContent(innerReadme).please();

    Note inInner = makeMe.aNote().folder(inner).please();

    NoteRealm realm = noteRealmService.build(inInner, user);

    assertThat(realm.getScopedReadmeContent(), equalTo(innerReadme));
    List<String> blocks = noteRealmService.questionGenerationInstructionBlocks(inInner);
    assertThat(blocks, hasSize(3));
    assertThat(blocks.get(0), containsString("nb-text"));
    assertThat(blocks.get(1), containsString("outer-text"));
    assertThat(blocks.get(2), containsString("inner-text"));
  }

  @Test
  void question_instruction_omits_levels_without_instruction() {
    Folder outer = makeMe.aFolder().notebook(notebook).name("Outer").please();
    String outerReadme =
        "---\ntitle_pattern: \"outer\"\nquestion_generation_instruction: outer-only\n---\n";
    makeMe.theFolder(outer).readmeContent(outerReadme).please();

    Folder inner = makeMe.aFolder().parentFolder(outer).name("Inner").please();
    makeMe.theFolder(inner).readmeContent("---\nother: x\n---\n").please();

    Note inInner = makeMe.aNote().folder(inner).please();

    NoteRealm realm = noteRealmService.build(inInner, user);

    assertThat(realm.getScopedReadmeContent(), equalTo(outerReadme));
    List<String> blocks = noteRealmService.questionGenerationInstructionBlocks(inInner);
    assertThat(blocks, hasSize(1));
    assertThat(blocks.get(0), containsString("outer-only"));
  }

  @Test
  void question_instruction_deduplicates_identical_text_at_multiple_levels() {
    makeMe
        .theNotebook(notebook)
        .readmeContent("---\nquestion_generation_instruction: shared-text\n---\n")
        .please();

    Folder outer = makeMe.aFolder().notebook(notebook).name("Outer").please();
    makeMe
        .theFolder(outer)
        .readmeContent("---\nquestion_generation_instruction: shared-text\n---\n")
        .please();

    Note inOuter = makeMe.aNote().folder(outer).please();

    List<String> blocks = noteRealmService.questionGenerationInstructionBlocks(inOuter);
    assertThat(blocks, hasSize(1));
    assertThat(blocks.get(0), containsString("Instruction from notebook"));
  }

  @Test
  void scoped_readme_content_from_notebook_when_folder_readme_has_no_title_pattern() {
    String nbContent = "---\ntitle_pattern: \"nb\"\n---\n";
    makeMe.theNotebook(notebook).readmeContent(nbContent).please();

    Folder folder = makeMe.aFolder().notebook(notebook).please();
    makeMe.theFolder(folder).readmeContent("---\n---\n").please();

    Note inFolder = makeMe.aNote().folder(folder).please();

    assertThat(noteRealmService.build(inFolder, user).getScopedReadmeContent(), equalTo(nbContent));
  }

  @Test
  void scoped_metadata_absent_when_notebook_has_no_matching_frontmatter() {
    Note normal = makeMe.aNote().notebook(notebook).please();

    NoteRealm realm = noteRealmService.build(normal, user);

    assertThat(realm.getScopedReadmeContent(), nullValue());
    assertThat(noteRealmService.questionGenerationInstructionBlocks(normal), empty());
  }

  @Test
  void sibling_note_frontmatter_does_not_supply_scoped_readme_metadata() {
    makeMe
        .aNote()
        .notebook(notebook)
        .title("sibling_with_pattern")
        .content(
            "---\ntitle_pattern: \"{{date}}\"\nquestion_generation_instruction: should not appear\n---\n")
        .please();
    Note normal = makeMe.aNote().notebook(notebook).please();

    NoteRealm realm = noteRealmService.build(normal, user);

    assertThat(realm.getScopedReadmeContent(), nullValue());
    assertThat(noteRealmService.questionGenerationInstructionBlocks(normal), empty());
  }

  @Test
  void question_instruction_blocks_label_the_focus_note_frontmatter() {
    Note note =
        makeMe
            .aNote()
            .notebook(notebook)
            .content("---\nquestion_generation_instruction: Note-level text\n---\nBody")
            .please();

    List<String> blocks = noteRealmService.questionGenerationInstructionBlocks(note);
    assertThat(blocks.get(0), containsString("Instruction from the focus note:"));
    assertThat(blocks.get(0), containsString("Note-level text"));
  }

  @Test
  void question_instruction_blocks_empty_when_absent() {
    Note note = makeMe.aNote().notebook(notebook).content("Body").please();

    assertThat(noteRealmService.questionGenerationInstructionBlocks(note), empty());
  }
}
