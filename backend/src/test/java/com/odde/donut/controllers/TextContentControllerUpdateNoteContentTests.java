package com.odde.donut.controllers;

import static com.odde.donut.entities.repositories.AuthoredNoteReferenceRowTestSupport.rowsFor;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;

import com.odde.donut.controllers.dto.NoteRealm;
import com.odde.donut.controllers.dto.WikiLink;
import com.odde.donut.entities.AttachmentBlob;
import com.odde.donut.entities.Image;
import com.odde.donut.entities.Note;
import com.odde.donut.exceptions.UnexpectedNoAccessRightException;
import jakarta.persistence.EntityManager;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

class TextContentControllerUpdateNoteContentTests extends TextContentControllerTestBase {
  private static final String ORDINARY_NOTE_FENCE = "---\ntype: Note\n---\n";

  @Autowired EntityManager entityManager;

  @Test
  void shouldBeAbleToSaveNoteWhenValid() throws UnexpectedNoAccessRightException {
    NoteRealm response = controller.updateNoteContent(note, contentDto("new content"));
    assertThat(response.getId(), equalTo(note.getId()));
    assertThat(response.getNote().getContent(), equalTo(ORDINARY_NOTE_FENCE + "new content"));
  }

  @Test
  void preservesLeadingYamlFrontmatterInContent() throws UnexpectedNoAccessRightException {
    String contentWithFrontmatter =
        """
        ---
        type: Note
        key_one: alpha
        key_two: beta
        ---

        # Body heading

        Paragraph content.
        """;
    assertThat(
        controller
            .updateNoteContent(note, contentDto(contentWithFrontmatter))
            .getNote()
            .getContent(),
        equalTo(contentWithFrontmatter));
  }

  @Test
  void liveResolvesWikiLinkWhenContentChangesTarget() throws UnexpectedNoAccessRightException {
    Note onlyA = makeMe.aNote().title("OnlyA").notebookOwnedBy(currentUser.getUser()).please();
    Note onlyB = makeMe.aNote().title("OnlyB").underSameNotebookAs(onlyA).please();
    Note carrier = makeMe.aNote().underSameNotebookAs(onlyA).please();

    controller.updateNoteContent(carrier, contentDto("[[OnlyA]]"));
    NoteRealm response = controller.updateNoteContent(carrier, contentDto("[[OnlyB]]"));

    assertThat(response.getWikiLinks(), hasSize(1));
    WikiLink wt = response.getWikiLinks().getFirst();
    assertThat(wt.getAuthoredLink(), equalTo("OnlyB"));
    assertThat(wt.getTarget(), equalTo("OnlyB"));
    assertThat(wt.getDisplayText(), equalTo("OnlyB"));
    assertThat(wt.getDestinationNoteId(), equalTo(onlyB.getId()));

    assertThat(rowsFor(entityManager, carrier), hasSize(1));
    assertThat(rowsFor(entityManager, carrier).getFirst().getAuthoredLink(), equalTo("OnlyB"));
  }

  @Test
  void liveResolvesWikiLinkWithDisplayText() throws UnexpectedNoAccessRightException {
    Note onlyA = makeMe.aNote().title("OnlyA").notebookOwnedBy(currentUser.getUser()).please();
    Note carrier = makeMe.aNote().underSameNotebookAs(onlyA).please();

    NoteRealm response = controller.updateNoteContent(carrier, contentDto("[[OnlyA|alias label]]"));

    WikiLink wt = response.getWikiLinks().getFirst();
    assertThat(wt.getAuthoredLink(), equalTo("OnlyA|alias label"));
    assertThat(wt.getDisplayText(), equalTo("alias label"));
    assertThat(wt.getDestinationNoteId(), equalTo(onlyA.getId()));
  }

  @Test
  void clearsWikiLinksWhenContentBecomeBlank() throws UnexpectedNoAccessRightException {
    Note onlyA = makeMe.aNote().title("OnlyA").notebookOwnedBy(currentUser.getUser()).please();
    Note carrier = makeMe.aNote().underSameNotebookAs(onlyA).please();

    controller.updateNoteContent(carrier, contentDto("[[OnlyA]]"));

    NoteRealm response = controller.updateNoteContent(carrier, contentDto(""));

    assertThat(response.getWikiLinks(), empty());
    assertThat(rowsFor(entityManager, carrier), empty());
  }

  @Test
  void deletesOrphanImagesWhenContentReferencesSingleAttachmentPath()
      throws UnexpectedNoAccessRightException {
    Image kept = makeMe.anImage().forNote(note).please();
    Image orphan = makeMe.anImage().forNote(note).please();
    Image otherNoteImage = makeMe.anImage().forNote(makeMe.aNote().please()).please();

    controller.updateNoteContent(
        note,
        contentDto(
            "---\nimage: /attachments/images/"
                + kept.getId()
                + "/"
                + kept.getName()
                + "\n---\nbody"));

    assertThat(entityManager.find(Image.class, kept.getId()), notNullValue());
    assertThat(entityManager.find(Image.class, orphan.getId()), nullValue());
    assertThat(entityManager.find(AttachmentBlob.class, orphan.getBlob().getId()), nullValue());
    assertThat(entityManager.find(AttachmentBlob.class, kept.getBlob().getId()), notNullValue());
    assertThat(entityManager.find(Image.class, otherNoteImage.getId()), notNullValue());
  }

  @Test
  void deletesAllNoteImagesWhenFrontmatterHasNoImageScalar()
      throws UnexpectedNoAccessRightException {
    Image first = makeMe.anImage().forNote(note).please();
    Image second = makeMe.anImage().forNote(note).please();

    controller.updateNoteContent(note, contentDto("just markdown"));

    assertThat(entityManager.find(Image.class, first.getId()), nullValue());
    assertThat(entityManager.find(Image.class, second.getId()), nullValue());
  }

  @Test
  void skipsOrphanCleanupWhenImageScalarIsNotCanonicalAttachmentPath()
      throws UnexpectedNoAccessRightException {
    Image first = makeMe.anImage().forNote(note).please();
    Image second = makeMe.anImage().forNote(note).please();

    controller.updateNoteContent(
        note, contentDto("---\nimage: https://example.com/a.png\n---\nbody"));

    assertThat(entityManager.find(Image.class, first.getId()), notNullValue());
    assertThat(entityManager.find(Image.class, second.getId()), notNullValue());
  }

  @Test
  void storesFullEncodedPropertyWikiTokenWhenExactPropertyExists()
      throws UnexpectedNoAccessRightException {
    Note moon = noteWithExactProperty("Moon", "a part of");
    Note carrier = makeMe.aNote().underSameNotebookAs(moon).please();

    NoteRealm response =
        controller.updateNoteContent(carrier, contentDto("[[Moon#prop:a%20part%20of]]"));

    assertThat(response.getWikiLinks(), hasSize(1));
    WikiLink wt = response.getWikiLinks().getFirst();
    assertThat(wt.getAuthoredLink(), equalTo("Moon#prop:a%20part%20of"));
    assertThat(wt.getTarget(), equalTo("Moon#prop:a%20part%20of"));
    assertThat(wt.getDestinationNoteId(), equalTo(moon.getId()));

    assertThat(rowsFor(entityManager, carrier), hasSize(1));
    assertThat(
        rowsFor(entityManager, carrier).getFirst().getAuthoredLink(),
        equalTo("Moon#prop:a%20part%20of"));
  }

  @Test
  void ignoresFileLookingPropertyMarkdownHrefWhenExactPropertyExists()
      throws UnexpectedNoAccessRightException {
    Note moon = noteWithExactProperty("Moon", "a part of");
    Note carrier = makeMe.aNote().underSameNotebookAs(moon).please();

    NoteRealm response =
        controller.updateNoteContent(
            carrier, contentDto("[a part of](/Moon.md#prop:a%20part%20of)"));

    assertThat(response.getWikiLinks(), empty());
  }

  @Test
  void omitsPropertyWikiTokenWhenExactPropertyDoesNotExist()
      throws UnexpectedNoAccessRightException {
    Note moon = makeMe.aNote().title("Moon").notebookOwnedBy(currentUser.getUser()).please();
    Note carrier = makeMe.aNote().underSameNotebookAs(moon).please();

    NoteRealm response =
        controller.updateNoteContent(carrier, contentDto("[[Moon#prop:a%20part%20of]]"));

    assertThat(response.getWikiLinks(), empty());
  }

  @Test
  void storesBothCaseDistinctPropertyWikiTokensWhenBothKeysExist()
      throws UnexpectedNoAccessRightException {
    Note moon =
        makeMe
            .aNote()
            .title("Moon")
            .notebookOwnedBy(currentUser.getUser())
            .content("---\nName: v\nname: w\n---\n")
            .please();
    Note carrier = makeMe.aNote().underSameNotebookAs(moon).please();

    NoteRealm response =
        controller.updateNoteContent(carrier, contentDto("[[Moon#prop:Name]] [[Moon#prop:name]]"));

    assertThat(
        response.getWikiLinks().stream().map(WikiLink::getAuthoredLink).toList(),
        equalTo(List.of("Moon#prop:Name", "Moon#prop:name")));
    assertThat(
        rowsFor(entityManager, carrier).stream().map(row -> row.getAuthoredLink()).toList(),
        equalTo(List.of("Moon#prop:Name", "Moon#prop:name")));
  }
}
