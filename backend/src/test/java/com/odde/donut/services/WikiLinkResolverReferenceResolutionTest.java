package com.odde.donut.services;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.instanceOf;

import com.odde.donut.algorithms.AuthoredNoteReference;
import com.odde.donut.algorithms.NoteReferenceResolution;
import com.odde.donut.entities.Note;
import com.odde.donut.entities.Notebook;
import com.odde.donut.entities.User;
import com.odde.donut.testability.MakeMe;
import java.sql.Timestamp;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

/**
 * The domain-stable {@link WikiLinkResolver#resolveReference} entry point: one resolution result
 * for both wiki Portable-path and note-ID URL authored references.
 */
@SpringBootTest
@ActiveProfiles("test")
@Transactional
class WikiLinkResolverReferenceResolutionTest {

  @Autowired MakeMe makeMe;
  @Autowired WikiLinkResolver wikiLinkResolver;

  @Test
  void resolveReference_resolvesWikiPortablePathTarget_whenUniqueReadableMatch() {
    User owner = makeMe.aUser().please();
    Note target = makeMe.aNote().title("Alpha").notebookOwnedBy(owner).please();
    Note source = makeMe.aNote().underSameNotebookAs(target).please();

    NoteReferenceResolution resolution =
        wikiLinkResolver.resolveReference(
            AuthoredNoteReference.WikiPortablePathTarget.fromAuthoredInner("Alpha"), source, owner);

    assertThat(resolution, instanceOf(NoteReferenceResolution.Resolved.class));
    assertThat(
        ((NoteReferenceResolution.Resolved) resolution).destinationNote().getId(),
        equalTo(target.getId()));
  }

  @Test
  void resolveReference_returnsMissing_whenWikiPortablePathTargetHasNoMatch() {
    User owner = makeMe.aUser().please();
    Note source = makeMe.aNote().notebookOwnedBy(owner).please();

    NoteReferenceResolution resolution =
        wikiLinkResolver.resolveReference(
            AuthoredNoteReference.WikiPortablePathTarget.fromAuthoredInner("NoSuchTitle"),
            source,
            owner);

    assertThat(resolution, instanceOf(NoteReferenceResolution.Missing.class));
  }

  @Test
  void resolveReference_returnsAmbiguous_whenWikiPortablePathTargetHasSeveralReadableMatches() {
    User owner = makeMe.aUser().please();
    Notebook otherNotebook =
        makeMe.aNotebook().creatorAndOwner(owner).name("Other Notebook").please();
    Note first = makeMe.aNote().title("first").notebook(otherNotebook).aliases("term").please();
    makeMe.aNote().title("second").underSameNotebookAs(first).aliases("term").please();
    Note source = makeMe.aNote().notebookOwnedBy(owner).please();

    NoteReferenceResolution resolution =
        wikiLinkResolver.resolveReference(
            AuthoredNoteReference.WikiPortablePathTarget.fromAuthoredInner("Other Notebook:term"),
            source,
            owner);

    assertThat(resolution, instanceOf(NoteReferenceResolution.Ambiguous.class));
  }

  @Test
  void resolveReference_resolvesNoteIdUrlTarget_whenViewerMayReadTargetNotebook() {
    User owner = makeMe.aUser().please();
    Note target = makeMe.aNote().notebookOwnedBy(owner).please();
    Note source = makeMe.aNote().underSameNotebookAs(target).please();

    NoteReferenceResolution resolution =
        wikiLinkResolver.resolveReference(
            new AuthoredNoteReference.NoteIdUrlTarget(
                "[label](/n" + target.getId() + ")",
                target.getId(),
                "/n" + target.getId(),
                "label"),
            source,
            owner);

    assertThat(resolution, instanceOf(NoteReferenceResolution.Resolved.class));
    assertThat(
        ((NoteReferenceResolution.Resolved) resolution).destinationNote().getId(),
        equalTo(target.getId()));
  }

  @Test
  void resolveReference_returnsMissing_whenNoteIdUrlTargetIsSoftDeleted() {
    User owner = makeMe.aUser().please();
    Note target = makeMe.aNote().notebookOwnedBy(owner).please();
    Note source = makeMe.aNote().underSameNotebookAs(target).please();
    target.setDeletedAt(new Timestamp(System.currentTimeMillis()));
    makeMe.entityPersister.merge(target);

    NoteReferenceResolution resolution =
        wikiLinkResolver.resolveReference(
            new AuthoredNoteReference.NoteIdUrlTarget(
                "[label](/n" + target.getId() + ")",
                target.getId(),
                "/n" + target.getId(),
                "label"),
            source,
            owner);

    assertThat(resolution, instanceOf(NoteReferenceResolution.Missing.class));
  }

  @Test
  void resolveReference_returnsMissing_whenViewerMayNotReadNoteIdUrlTargetNotebook() {
    User owner = makeMe.aUser().please();
    Note target = makeMe.aNote().notebookOwnedBy(owner).please();
    Note source = makeMe.aNote().underSameNotebookAs(target).please();
    User unrelatedViewer = makeMe.aUser().please();

    NoteReferenceResolution resolution =
        wikiLinkResolver.resolveReference(
            new AuthoredNoteReference.NoteIdUrlTarget(
                "[label](/n" + target.getId() + ")",
                target.getId(),
                "/n" + target.getId(),
                "label"),
            source,
            unrelatedViewer);

    assertThat(resolution, instanceOf(NoteReferenceResolution.Missing.class));
  }
}
