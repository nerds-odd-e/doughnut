package com.odde.donut.entities.repositories;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;

import com.odde.donut.algorithms.AuthoredNoteDocument;
import com.odde.donut.algorithms.AuthoredNoteReference;
import com.odde.donut.entities.Note;
import com.odde.donut.entities.User;
import com.odde.donut.testability.MakeMe;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

/**
 * {@link AuthoredNoteReferenceInboundFacade}: live inbound-candidate selection and resolution
 * against {@code authored_note_reference} rows. Consumed by {@code
 * com.odde.donut.services.NoteRealmService} for {@link
 * com.odde.donut.controllers.dto.NoteRealm#getReferences()}.
 */
@SpringBootTest
@ActiveProfiles("test")
@Transactional
class AuthoredNoteReferenceInboundFacadeTest {

  @Autowired MakeMe makeMe;
  @Autowired AuthoredNoteReferenceInboundFacade authoredNoteReferenceInboundFacade;

  @Test
  void includesWikiAndNoteIdUrlReferrersOrderedByReferrerNoteIdAscending() {
    User owner = makeMe.aUser().please();
    Note target = makeMe.aNote().title("Alpha").notebookOwnedBy(owner).please();
    Note sourceWithLowerId = makeMe.aNote().underSameNotebookAs(target).please();
    Note sourceWithHigherId = makeMe.aNote().underSameNotebookAs(target).please();
    // Author the higher-id source first, to prove the result is ordered by referrer note id and
    // not by authoring/call order.
    sourceWithHigherId.replaceContent(
        new AuthoredNoteDocument(
            "references Alpha by id",
            List.of(
                new AuthoredNoteReference.NoteIdUrlTarget(
                    "[Alpha](/n" + target.getId() + ")",
                    target.getId(),
                    "/n" + target.getId(),
                    "Alpha"))));
    sourceWithLowerId.replaceContent(
        new AuthoredNoteDocument(
            "references [[Alpha]]",
            List.of(AuthoredNoteReference.WikiPortablePathTarget.fromAuthoredInner("Alpha"))));
    makeMe.entityPersister.flush();

    List<Note> referrers =
        authoredNoteReferenceInboundFacade.distinctReferrerNotesForViewer(target, owner);

    assertThat(
        referrers.stream().map(Note::getId).toList(),
        contains(sourceWithLowerId.getId(), sourceWithHigherId.getId()));
  }

  @Test
  void excludesReferrerWhoseAuthoredReferenceResolvesToADifferentNote() {
    User owner = makeMe.aUser().please();
    Note target = makeMe.aNote().title("Alpha").notebookOwnedBy(owner).please();
    makeMe.aNote().title("Beta").underSameNotebookAs(target).please();
    Note source = makeMe.aNote().underSameNotebookAs(target).please();
    source.replaceContent(
        new AuthoredNoteDocument(
            "references [[Beta]]",
            List.of(AuthoredNoteReference.WikiPortablePathTarget.fromAuthoredInner("Beta"))));
    makeMe.entityPersister.flush();

    List<Note> referrers =
        authoredNoteReferenceInboundFacade.distinctReferrerNotesForViewer(target, owner);

    assertThat(referrers, empty());
  }

  @Test
  void deduplicatesOneReferrerAuthoringSeveralReferencesToTheSameTarget() {
    User owner = makeMe.aUser().please();
    Note target = makeMe.aNote().title("Alpha").notebookOwnedBy(owner).please();
    Note source = makeMe.aNote().underSameNotebookAs(target).please();
    source.replaceContent(
        new AuthoredNoteDocument(
            "wiki and id both point at Alpha",
            List.of(
                AuthoredNoteReference.WikiPortablePathTarget.fromAuthoredInner("Alpha"),
                new AuthoredNoteReference.NoteIdUrlTarget(
                    "[Alpha](/n" + target.getId() + ")",
                    target.getId(),
                    "/n" + target.getId(),
                    "Alpha"))));
    makeMe.entityPersister.flush();

    List<Note> referrers =
        authoredNoteReferenceInboundFacade.distinctReferrerNotesForViewer(target, owner);

    assertThat(referrers, hasSize(1));
    assertThat(referrers.getFirst().getId(), equalTo(source.getId()));
  }

  @Test
  void matchesAWikiReferenceAuthoredAgainstTheTargetsCurrentAlias() {
    User owner = makeMe.aUser().please();
    Note target = makeMe.aNote().title("Alpha").notebookOwnedBy(owner).aliases("nickname").please();
    Note source = makeMe.aNote().underSameNotebookAs(target).please();
    source.replaceContent(
        new AuthoredNoteDocument(
            "references [[nickname]]",
            List.of(AuthoredNoteReference.WikiPortablePathTarget.fromAuthoredInner("nickname"))));
    makeMe.entityPersister.flush();

    List<Note> referrers =
        authoredNoteReferenceInboundFacade.distinctReferrerNotesForViewer(target, owner);

    assertThat(referrers.stream().map(Note::getId).toList(), contains(source.getId()));
  }

  @Test
  void excludesAReferenceThatIsUnreadableToTheViewer() {
    User owner = makeMe.aUser().please();
    Note target = makeMe.aNote().title("Alpha").notebookOwnedBy(owner).please();
    Note source = makeMe.aNote().underSameNotebookAs(target).please();
    source.replaceContent(
        new AuthoredNoteDocument(
            "references [[Alpha]]",
            List.of(AuthoredNoteReference.WikiPortablePathTarget.fromAuthoredInner("Alpha"))));
    makeMe.entityPersister.flush();
    User unrelatedViewer = makeMe.aUser().please();

    List<Note> referrers =
        authoredNoteReferenceInboundFacade.distinctReferrerNotesForViewer(target, unrelatedViewer);

    assertThat(referrers, empty());
  }
}
