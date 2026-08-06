package com.odde.doughnut.services;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;

import com.odde.doughnut.controllers.dto.NoteRealm;
import com.odde.doughnut.entities.Note;
import com.odde.doughnut.entities.NoteWikiTitleCache;
import com.odde.doughnut.entities.Notebook;
import com.odde.doughnut.entities.User;
import com.odde.doughnut.entities.repositories.NoteWikiTitleCacheRepository;
import com.odde.doughnut.testability.MakeMe;
import java.sql.Timestamp;
import java.util.stream.Stream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
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
  Notebook notebook;

  @BeforeEach
  void defaultNotebook() {
    user = makeMe.aUser().please();
    notebook = makeMe.aNotebook().creatorAndOwner(user).please();
  }

  @Test
  void wiki_titles_empty_when_content_has_links_but_cache_not_refreshed() {
    makeMe.aNote().title("LinkedPage").notebook(notebook).please();
    Note carrier = makeMe.aNote().notebook(notebook).content("[[LinkedPage]]").please();

    assertThat(noteRealmService.build(carrier, user).getWikiTitles(), empty());
  }

  @Test
  void omits_cached_target_when_viewer_cannot_read_target_notebook() {
    User otherUser = makeMe.aUser().please();
    Notebook secretNb = makeMe.aNotebook().creatorAndOwner(otherUser).name("SecretNb").please();
    Note hidden = makeMe.aNote().title("Hidden").notebook(secretNb).please();

    User viewer = makeMe.aUser().please();
    Note carrier = makeMe.aNote().notebookOwnedBy(viewer).content("plain").please();

    persistWikiLink(carrier, hidden, "SecretNb:Hidden");

    assertThat(noteRealmService.build(carrier, viewer).getWikiTitles(), empty());
  }

  @Test
  void omits_cached_target_when_target_note_is_soft_deleted() {
    Note target = makeMe.aNote().title("Target").notebook(notebook).please();
    Note carrier = makeMe.aNote().notebook(notebook).content("[[Target]]").please();
    wikiTitleCacheService.refreshForNote(carrier, user);

    softDelete(target);

    assertThat(noteRealmService.build(carrier, user).getWikiTitles(), empty());
  }

  @Test
  void references_empty_when_cache_rows_deleted_for_relation_carrier() {
    Note focal = makeMe.aNote().title("Focal").notebook(notebook).please();
    Note subject = makeMe.aNote().notebook(notebook).please();
    Note relation =
        makeMe.aNote().notebook(notebook).withWikiLinksInFrontmatter(subject, focal).please();
    noteWikiTitleCacheRepository.deleteByNote_Id(relation.getId());

    assertThat(noteRealmService.build(focal, user).getReferences(), empty());
  }

  @Test
  void body_wikilink_carrier_in_references() {
    Note focal = makeMe.aNote().title("Focal").notebook(notebook).please();
    Note carrier = makeMe.aNote().notebook(notebook).content("[[Focal]]").please();
    wikiTitleCacheService.refreshForNote(carrier, user);

    NoteRealm realm = noteRealmService.build(focal, user);

    assertThat(realm.getReferences(), hasSize(1));
    assertThat(realm.getReferences().get(0).getId(), equalTo(carrier.getId()));
  }

  @Test
  void parent_yaml_carrier_appears_in_references() {
    Note focal = makeMe.aNote().title("Focal").notebook(notebook).please();
    Note carrier =
        makeMe
            .aNote()
            .title("Child")
            .notebook(notebook)
            .content("---\nparent: \"[[Focal]]\"\n---\n\nBody.")
            .please();
    wikiTitleCacheService.refreshForNote(carrier, user);

    NoteRealm realm = noteRealmService.build(focal, user);

    assertThat(realm.getReferences(), hasSize(1));
    assertThat(realm.getReferences().get(0).getId(), equalTo(carrier.getId()));
  }

  @Test
  void references_omit_soft_deleted_relation_even_if_cache_row_remains() {
    Note focal = makeMe.aNote().title("Focal").notebook(notebook).please();
    Note subject = makeMe.aNote().notebook(notebook).please();
    Note relation =
        makeMe.aNote().notebook(notebook).withWikiLinksInFrontmatter(subject, focal).please();
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
    Notebook mainNb = makeMe.aNotebook().creatorAndOwner(focalOwner).name("MainNb").please();
    Note focal = makeMe.aNote().title("Focal").notebook(mainNb).please();
    Notebook otherNb = makeMe.aNotebook().creatorAndOwner(carrierOwner).name("OtherNb").please();
    Note carrier = makeMe.aNote().notebook(otherNb).please();

    persistWikiLink(carrier, focal, "MainNb:Focal");

    NoteRealm realm = noteRealmService.build(focal, focalOwner);

    assertThat(realm.getReferences(), hasSize(expectedReferences));
    if (expectedReferences == 1) {
      assertThat(realm.getReferences().get(0).getId(), equalTo(carrier.getId()));
    }
  }

  @Test
  void references_omit_soft_deleted_carrier_even_if_cache_row_remains() {
    Note focal = makeMe.aNote().title("Focal").notebook(notebook).please();
    Note carrier = makeMe.aNote().notebook(notebook).content("[[Focal]]").please();
    wikiTitleCacheService.refreshForNote(carrier, user);

    softDelete(carrier);

    assertThat(noteRealmService.build(focal, user).getReferences(), empty());
  }

  @Test
  void references_dedupe_multiple_cache_rows_for_same_carrier_note() {
    Note focal = makeMe.aNote().title("Focal").notebook(notebook).please();
    Note carrier = makeMe.aNote().notebook(notebook).please();

    persistWikiLink(carrier, focal, "one");
    persistWikiLink(carrier, focal, "two");

    assertThat(noteRealmService.build(focal, user).getReferences(), hasSize(1));
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
