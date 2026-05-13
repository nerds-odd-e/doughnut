package com.odde.doughnut.services;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;

import com.odde.doughnut.controllers.dto.NoteRealm;
import com.odde.doughnut.entities.Folder;
import com.odde.doughnut.entities.Note;
import com.odde.doughnut.entities.NoteWikiTitleCache;
import com.odde.doughnut.entities.Notebook;
import com.odde.doughnut.entities.User;
import com.odde.doughnut.entities.repositories.NoteWikiTitleCacheRepository;
import com.odde.doughnut.testability.MakeMe;
import java.sql.Timestamp;
import java.util.Optional;
import java.util.stream.Stream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class NoteRealmServiceTest {

  @Autowired MakeMe makeMe;
  @Autowired NoteRealmService noteRealmService;
  @Autowired NoteWikiTitleCacheRepository noteWikiTitleCacheRepository;
  @Autowired WikiTitleCacheService wikiTitleCacheService;

  User user;
  Note root;
  Notebook notebook;

  @BeforeEach
  void defaultNotebook() {
    user = makeMe.aUser().please();
    root = makeMe.aNote().nbCreatorAndOwner(user).please();
    notebook = root.getNotebook();
  }

  @Test
  void wiki_titles_empty_when_content_has_links_but_cache_not_refreshed() {
    makeMe.aNote().title("LinkedPage").underSameNotebookAs(root).please();
    Note carrier = makeMe.aNote().underSameNotebookAs(root).content("[[LinkedPage]]").please();

    assertThat(noteRealmService.build(carrier, user).getWikiTitles(), empty());
  }

  @Test
  void omits_cached_target_when_viewer_cannot_read_target_notebook() {
    User otherUser = makeMe.aUser().please();
    Note headSecret = makeMe.aNote().nbCreatorAndOwner(otherUser).title("SecretNb").please();
    Note hidden = makeMe.aNote().title("Hidden").underSameNotebookAs(headSecret).please();

    User viewer = makeMe.aUser().please();
    Note viewerRoot = makeMe.aNote().nbCreatorAndOwner(viewer).please();
    Note carrier = makeMe.aNote().underSameNotebookAs(viewerRoot).content("plain").please();

    persistWikiLink(carrier, hidden, "SecretNb:Hidden");

    assertThat(noteRealmService.build(carrier, viewer).getWikiTitles(), empty());
  }

  @Test
  void omits_cached_target_when_target_note_is_soft_deleted() {
    Note target = makeMe.aNote().title("Target").creator(user).underSameNotebookAs(root).please();
    Note carrier = makeMe.aNote().underSameNotebookAs(root).content("[[Target]]").please();
    wikiTitleCacheService.refreshForNote(carrier, user);

    softDelete(target);

    assertThat(noteRealmService.build(carrier, user).getWikiTitles(), empty());
  }

  @Test
  void references_empty_when_cache_rows_deleted_for_relation_carrier() {
    Note focal = makeMe.aNote().title("Focal").underSameNotebookAs(root).please();
    Note subject = makeMe.aNote().underSameNotebookAs(root).please();
    Note relation = makeMe.aNote().withWikiLinksInFrontmatter(subject, focal).please();
    noteWikiTitleCacheRepository.deleteByNote_Id(relation.getId());

    assertThat(noteRealmService.build(focal, user).getReferences(), empty());
  }

  @Test
  void body_wikilink_carrier_in_references() {
    Note focal = makeMe.aNote().title("Focal").creator(user).underSameNotebookAs(root).please();
    Note carrier =
        makeMe.aNote().creator(user).underSameNotebookAs(root).content("[[Focal]]").please();
    wikiTitleCacheService.refreshForNote(carrier, user);

    NoteRealm realm = noteRealmService.build(focal, user);

    assertThat(realm.getReferences(), hasSize(1));
    assertThat(realm.getReferences().get(0).getId(), equalTo(carrier.getId()));
  }

  @Test
  void parent_yaml_carrier_appears_in_references() {
    Note focal = makeMe.aNote().title("Focal").creator(user).underSameNotebookAs(root).please();
    Note carrier = makeMe.aNote().title("Child").creator(user).underSameNotebookAs(root).please();
    carrier.setContent("---\nparent: \"[[Focal]]\"\n---\n\nBody.");
    makeMe.entityPersister.merge(carrier);
    makeMe.entityPersister.flush();
    wikiTitleCacheService.refreshForNote(carrier, user);

    NoteRealm realm = noteRealmService.build(focal, user);

    assertThat(realm.getReferences(), hasSize(1));
    assertThat(realm.getReferences().get(0).getId(), equalTo(carrier.getId()));
  }

  @Test
  void references_omit_soft_deleted_relation_even_if_cache_row_remains() {
    Note focal = makeMe.aNote().title("Focal").underSameNotebookAs(root).please();
    Note subject = makeMe.aNote().underSameNotebookAs(root).please();
    Note relation = makeMe.aNote().withWikiLinksInFrontmatter(subject, focal).please();
    relation.setContent(
        RelationshipNoteMarkdownFormatter.formatForRelationshipNote(
            relation, "a specialization of", subject, focal, null));
    makeMe.entityPersister.merge(relation);
    makeMe.entityPersister.flush();
    wikiTitleCacheService.refreshForNote(relation, user);

    softDelete(relation);

    assertThat(noteRealmService.build(subject, user).getReferences(), empty());
  }

  static Stream<Arguments> crossNotebookCarrierCases() {
    return Stream.of(Arguments.of(true, 1), Arguments.of(false, 0));
  }

  @ParameterizedTest
  @MethodSource("crossNotebookCarrierCases")
  void cross_notebook_carrier_references_respect_refer_rights(
      boolean carrierSharesOwnerWithViewer, int expectedReferences) {
    User focalOwner = makeMe.aUser().please();
    User carrierOwner = carrierSharesOwnerWithViewer ? focalOwner : makeMe.aUser().please();
    Note headMain = makeMe.aNote().nbCreatorAndOwner(focalOwner).title("MainNb").please();
    Note focal =
        makeMe.aNote().title("Focal").creator(focalOwner).underSameNotebookAs(headMain).please();
    Note headOther = makeMe.aNote().nbCreatorAndOwner(carrierOwner).title("OtherNb").please();
    Note carrier = makeMe.aNote().creator(carrierOwner).underSameNotebookAs(headOther).please();

    persistWikiLink(carrier, focal, "MainNb:Focal");

    NoteRealm realm = noteRealmService.build(focal, focalOwner);

    assertThat(realm.getReferences(), hasSize(expectedReferences));
    if (expectedReferences == 1) {
      assertThat(realm.getReferences().get(0).getId(), equalTo(carrier.getId()));
    }
  }

  @Test
  void references_omit_soft_deleted_carrier_even_if_cache_row_remains() {
    Note focal = makeMe.aNote().title("Focal").underSameNotebookAs(root).please();
    Note carrier = makeMe.aNote().underSameNotebookAs(root).content("[[Focal]]").please();
    wikiTitleCacheService.refreshForNote(carrier, user);

    softDelete(carrier);

    assertThat(noteRealmService.build(focal, user).getReferences(), empty());
  }

  @Test
  void references_dedupe_multiple_cache_rows_for_same_carrier_note() {
    Note focal = makeMe.aNote().title("Focal").creator(user).underSameNotebookAs(root).please();
    Note carrier = makeMe.aNote().creator(user).underSameNotebookAs(root).please();

    persistWikiLink(carrier, focal, "one");
    persistWikiLink(carrier, focal, "two");

    NoteRealm realm = noteRealmService.build(focal, user);

    assertThat(realm.getReferences(), hasSize(1));
    assertThat(realm.getReferences().get(0).getId(), equalTo(carrier.getId()));
  }

  @Test
  void ancestor_folders_ordered_outermost_to_innermost() {
    Folder outer = makeMe.aFolder().notebook(notebook).name("Outer").please();
    Folder inner = makeMe.aFolder().notebook(notebook).parentFolder(outer).name("Inner").please();
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
    assertThat(noteRealmService.build(root, user).getAncestorFolders(), empty());
  }

  @Test
  void notebook_index_applies_title_pattern_and_question_instruction_to_sibling_notes() {
    String indexContent =
        "---\ntitle_pattern: \"{{date}}\"\nquestion_generation_instruction: Focus on definitions\n---\n";
    makeMe.theNotebook(notebook).indexContent(indexContent).please();
    Note normal = makeMe.aNote().underSameNotebookAs(root).please();

    NoteRealm realm = noteRealmService.build(normal, user);

    assertThat(realm.getIndexNoteContent(), equalTo(indexContent));
    assertThat(
        noteRealmService.resolveScopedQuestionGenerationInstruction(normal),
        equalTo(Optional.of("Focus on definitions")));
  }

  @ParameterizedTest
  @CsvSource({
    "title_pattern, {{date}}",
    "titlePattern, {{date}}",
  })
  void index_note_content_recognizes_title_pattern_key_aliases(String key, String value) {
    String indexContent = "---\n" + key + ": \"" + value + "\"\n---\n";
    makeMe.theNotebook(notebook).indexContent(indexContent).please();
    Note normal = makeMe.aNote().underSameNotebookAs(root).please();

    assertThat(noteRealmService.build(normal, user).getIndexNoteContent(), equalTo(indexContent));
  }

  @ParameterizedTest
  @CsvSource({
    "question_generation_instruction, Focus on definitions",
    "questionGenerationInstruction, Legacy key text",
  })
  void scoped_question_instruction_recognizes_instruction_key_aliases(String key, String text) {
    makeMe.theNotebook(notebook).indexContent("---\n" + key + ": \"" + text + "\"\n---\n").please();
    Note normal = makeMe.aNote().underSameNotebookAs(root).please();

    assertThat(
        noteRealmService.resolveScopedQuestionGenerationInstruction(normal),
        equalTo(Optional.of(text)));
  }

  @Test
  void inner_folder_index_wins_for_title_pattern_and_question_instruction() {
    makeMe
        .theNotebook(notebook)
        .indexContent("---\ntitle_pattern: \"nb\"\nquestion_generation_instruction: nb\n---\n")
        .please();

    Folder outer = makeMe.aFolder().notebook(notebook).name("Outer").please();
    makeMe
        .theFolder(outer)
        .indexContent(
            "---\ntitle_pattern: \"outer\"\nquestion_generation_instruction: outer\n---\n")
        .please();

    Folder inner = makeMe.aFolder().notebook(notebook).parentFolder(outer).name("Inner").please();
    String innerIndex =
        "---\ntitle_pattern: \"inner\"\nquestion_generation_instruction: inner\n---\n";
    makeMe.theFolder(inner).indexContent(innerIndex).please();

    Note inInner = makeMe.aNote().folder(inner).please();

    NoteRealm realm = noteRealmService.build(inInner, user);

    assertThat(realm.getIndexNoteContent(), equalTo(innerIndex));
    assertThat(
        noteRealmService.resolveScopedQuestionGenerationInstruction(inInner),
        equalTo(Optional.of("inner")));
  }

  @Test
  void scoped_resolution_skips_inner_without_title_pattern_or_instruction_and_uses_parent() {
    Folder outer = makeMe.aFolder().notebook(notebook).name("Outer").please();
    String outerIndex =
        "---\ntitle_pattern: \"outer\"\nquestion_generation_instruction: outer-only\n---\n";
    makeMe.theFolder(outer).indexContent(outerIndex).please();

    Folder inner = makeMe.aFolder().notebook(notebook).parentFolder(outer).name("Inner").please();
    makeMe.theFolder(inner).indexContent("---\nother: x\n---\n").please();

    Note inInner = makeMe.aNote().folder(inner).please();

    NoteRealm realm = noteRealmService.build(inInner, user);

    assertThat(realm.getIndexNoteContent(), equalTo(outerIndex));
    assertThat(
        noteRealmService.resolveScopedQuestionGenerationInstruction(inInner),
        equalTo(Optional.of("outer-only")));
  }

  @Test
  void index_note_content_from_notebook_when_folder_index_content_has_no_title_pattern() {
    String nbContent = "---\ntitle_pattern: \"nb\"\n---\n";
    makeMe.theNotebook(notebook).indexContent(nbContent).please();

    Folder folder = makeMe.aFolder().notebook(notebook).please();
    makeMe.theFolder(folder).indexContent("---\n---\n").please();

    Note inFolder = makeMe.aNote().folder(folder).please();

    assertThat(noteRealmService.build(inFolder, user).getIndexNoteContent(), equalTo(nbContent));
  }

  @Test
  void scoped_metadata_absent_when_notebook_has_no_matching_frontmatter() {
    Note normal = makeMe.aNote().underSameNotebookAs(root).please();

    NoteRealm realm = noteRealmService.build(normal, user);

    assertThat(realm.getIndexNoteContent(), nullValue());
    assertThat(
        noteRealmService.resolveScopedQuestionGenerationInstruction(normal), is(Optional.empty()));
  }

  @Test
  void index_note_titled_for_deletion_does_not_supply_scoped_metadata() {
    makeMe
        .aNote()
        .inNotebook(notebook)
        .title("index_to_be_deleted")
        .content(
            "---\ntitle_pattern: \"{{date}}\"\nquestion_generation_instruction: should not appear\n---\n")
        .please();
    Note normal = makeMe.aNote().underSameNotebookAs(root).please();

    NoteRealm realm = noteRealmService.build(normal, user);

    assertThat(realm.getIndexNoteContent(), nullValue());
    assertThat(
        noteRealmService.resolveScopedQuestionGenerationInstruction(normal), is(Optional.empty()));
  }

  private void persistWikiLink(Note carrier, Note target, String linkText) {
    NoteWikiTitleCache row = new NoteWikiTitleCache();
    row.setNote(carrier);
    row.setTargetNote(target);
    row.setLinkText(linkText);
    noteWikiTitleCacheRepository.save(row);
  }

  private void softDelete(Note note) {
    note.setDeletedAt(new Timestamp(System.currentTimeMillis()));
    makeMe.entityPersister.merge(note);
  }
}
