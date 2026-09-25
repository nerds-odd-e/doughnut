package com.odde.donut.services;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;

import com.odde.donut.controllers.dto.NoteRealm;
import com.odde.donut.entities.Folder;
import com.odde.donut.entities.Note;
import com.odde.donut.entities.Notebook;
import com.odde.donut.entities.User;
import com.odde.donut.testability.RelationshipNoteMarkdown;
import com.odde.donut.testability.SpringTestBase;
import java.util.stream.Stream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;

class NoteRealmServiceTest extends SpringTestBase {
  @Autowired NoteRealmService noteRealmService;

  User user;
  Notebook notebook;

  @BeforeEach
  void defaultNotebook() {
    user = makeMe.aUser().please();
    notebook = makeMe.aNotebook().creatorAndOwner(user).please();
  }

  @Test
  void wiki_links_resolve_from_authored_content() {
    Note target = makeMe.aNote().title("LinkedPage").notebook(notebook).please();
    Note carrier = makeMe.aNote().notebook(notebook).content("[[LinkedPage]]").please();

    NoteRealm realm = noteRealmService.build(carrier, user);

    assertThat(realm.getWikiLinks(), hasSize(1));
    assertThat(realm.getWikiLinks().get(0).getDestinationNoteId(), equalTo(target.getId()));
  }

  @Test
  void omits_unreadable_target_notebook_from_outgoing_wiki_links() {
    User otherUser = makeMe.aUser().please();
    Notebook secretNb = makeMe.aNotebook().creatorAndOwner(otherUser).name("SecretNb").please();
    makeMe.aNote().title("Hidden").notebook(secretNb).please();

    User viewer = makeMe.aUser().please();
    Note carrier = makeMe.aNote().notebookOwnedBy(viewer).content("[[SecretNb:Hidden]]").please();

    assertThat(noteRealmService.build(carrier, viewer).getWikiLinks(), empty());
  }

  @Test
  void omits_target_when_target_note_is_trashed() {
    Note target = makeMe.aNote().title("Target").notebook(notebook).please();
    Note carrier = makeMe.aNote().notebook(notebook).content("[[Target]]").please();

    trashNote(target);

    assertThat(noteRealmService.build(carrier, user).getWikiLinks(), empty());
  }

  @Test
  void body_wikilink_carrier_in_references() {
    Note focal = makeMe.aNote().title("Focal").notebook(notebook).please();
    Note carrier = makeMe.aNote().notebook(notebook).please();
    makeMe.authorReferencingContent(carrier, "[[Focal]]");

    NoteRealm realm = noteRealmService.build(focal, user);

    assertThat(realm.getReferences(), hasSize(1));
    assertThat(realm.getReferences().get(0).getId(), equalTo(carrier.getId()));
  }

  @Test
  void parent_yaml_carrier_appears_in_references() {
    Note focal = makeMe.aNote().title("Focal").notebook(notebook).please();
    Note carrier = makeMe.aNote().title("Child").notebook(notebook).please();
    makeMe.authorReferencingContent(carrier, "---\nparent: \"[[Focal]]\"\n---\n\nBody.");

    NoteRealm realm = noteRealmService.build(focal, user);

    assertThat(realm.getReferences(), hasSize(1));
    assertThat(realm.getReferences().get(0).getId(), equalTo(carrier.getId()));
  }

  @Test
  void references_omit_trashed_relation() {
    Note focal = makeMe.aNote().title("Focal").notebook(notebook).please();
    Note subject = makeMe.aNote().notebook(notebook).please();
    Note relation = makeMe.aNote().notebook(notebook).please();
    makeMe.authorReferencingContent(
        relation,
        RelationshipNoteMarkdown.forEndpoints(
            relation, "a specialization of", subject, focal, null));

    trashNote(relation);

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

    makeMe.authorReferencingContent(carrier, "[[MainNb:Focal]]");

    NoteRealm realm = noteRealmService.build(focal, focalOwner);

    assertThat(realm.getReferences(), hasSize(expectedReferences));
    if (expectedReferences == 1) {
      assertThat(realm.getReferences().get(0).getId(), equalTo(carrier.getId()));
    }
  }

  @Test
  void references_omit_trashed_carrier() {
    Note focal = makeMe.aNote().title("Focal").notebook(notebook).please();
    Note carrier = makeMe.aNote().notebook(notebook).please();
    makeMe.authorReferencingContent(carrier, "[[Focal]]");

    trashNote(carrier);

    assertThat(noteRealmService.build(focal, user).getReferences(), empty());
  }

  @Test
  void references_dedupe_multiple_authored_references_for_same_carrier_note() {
    Note focal = makeMe.aNote().title("Focal").notebook(notebook).please();
    Note carrier = makeMe.aNote().notebook(notebook).please();
    makeMe.authorReferencingContent(carrier, "[[Focal]] and [[Focal|again]]");

    assertThat(noteRealmService.build(focal, user).getReferences(), hasSize(1));
  }

  private void trashNote(Note note) {
    Folder trash = makeMe.aFolder().notebook(note.getNotebook()).name("_trash").please();
    note.setFolder(trash);
    makeMe.entityPersister.merge(note);
  }
}
