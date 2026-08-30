package com.odde.donut.controllers;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;

import com.odde.donut.controllers.dto.NoteRealm;
import com.odde.donut.controllers.dto.WikiTitle;
import com.odde.donut.entities.Image;
import com.odde.donut.entities.Note;
import com.odde.donut.entities.NoteWikiTitleCache;
import com.odde.donut.entities.repositories.ImageRepository;
import com.odde.donut.entities.repositories.NoteWikiTitleCacheRepository;
import com.odde.donut.exceptions.UnexpectedNoAccessRightException;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

class TextContentControllerUpdateNoteContentTests extends TextContentControllerTestBase {
  private static final String ORDINARY_NOTE_FENCE = "---\ntype: Note\n---\n";

  @Autowired NoteWikiTitleCacheRepository noteWikiTitleCacheRepository;
  @Autowired ImageRepository imageRepository;

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
  void refreshesWikiTitleCacheWhenContentContainResolvedWikiLink()
      throws UnexpectedNoAccessRightException {
    Note onlyA = makeMe.aNote().title("OnlyA").notebookOwnedBy(currentUser.getUser()).please();
    Note onlyB = makeMe.aNote().title("OnlyB").underSameNotebookAs(onlyA).please();
    Note carrier = makeMe.aNote().underSameNotebookAs(onlyA).please();

    controller.updateNoteContent(carrier, contentDto("[[OnlyA]]"));
    NoteRealm response = controller.updateNoteContent(carrier, contentDto("[[OnlyB]]"));

    assertThat(response.getWikiTitles(), hasSize(1));
    WikiTitle wt = response.getWikiTitles().getFirst();
    assertThat(wt.getLinkText(), equalTo("OnlyB"));
    assertThat(wt.getTargetToken(), equalTo("OnlyB"));
    assertThat(wt.getDisplayText(), equalTo("OnlyB"));
    assertThat(wt.getNoteId(), equalTo(onlyB.getId()));

    List<NoteWikiTitleCache> rows =
        noteWikiTitleCacheRepository.findByNote_IdOrderByIdAsc(carrier.getId());
    assertThat(rows, hasSize(1));
    assertThat(rows.getFirst().getLinkText(), equalTo("OnlyB"));
    assertThat(rows.getFirst().getTargetNote().getId(), equalTo(onlyB.getId()));
  }

  @Test
  void refreshesWikiTitleCacheWhenContentHasResolvedWikiLinkWithDisplayText()
      throws UnexpectedNoAccessRightException {
    Note onlyA = makeMe.aNote().title("OnlyA").notebookOwnedBy(currentUser.getUser()).please();
    Note carrier = makeMe.aNote().underSameNotebookAs(onlyA).please();

    NoteRealm response = controller.updateNoteContent(carrier, contentDto("[[OnlyA|alias label]]"));

    WikiTitle wt = response.getWikiTitles().getFirst();
    assertThat(wt.getLinkText(), equalTo("OnlyA|alias label"));
    assertThat(wt.getDisplayText(), equalTo("alias label"));
    assertThat(wt.getNoteId(), equalTo(onlyA.getId()));
  }

  @Test
  void clearsWikiTitleCacheWhenContentBecomeBlank() throws UnexpectedNoAccessRightException {
    Note onlyA = makeMe.aNote().title("OnlyA").notebookOwnedBy(currentUser.getUser()).please();
    Note carrier = makeMe.aNote().underSameNotebookAs(onlyA).please();

    controller.updateNoteContent(carrier, contentDto("[[OnlyA]]"));

    NoteRealm response = controller.updateNoteContent(carrier, contentDto(""));

    assertThat(response.getWikiTitles(), empty());
    assertThat(noteWikiTitleCacheRepository.findByNote_IdOrderByIdAsc(carrier.getId()), empty());
  }

  @Test
  void deletesOrphanImagesWhenContentReferencesSingleAttachmentPath()
      throws UnexpectedNoAccessRightException {
    Image kept = makeMe.anImage().forNote(note).please();
    Image orphan = makeMe.anImage().forNote(note).please();

    controller.updateNoteContent(
        note,
        contentDto(
            "---\nimage: /attachments/images/"
                + kept.getId()
                + "/"
                + kept.getName()
                + "\n---\nbody"));

    assertThat(imageRepository.findById(kept.getId()).isPresent(), equalTo(true));
    assertThat(imageRepository.findById(orphan.getId()).isPresent(), equalTo(false));
  }

  @Test
  void deletesAllNoteImagesWhenFrontmatterHasNoImageScalar()
      throws UnexpectedNoAccessRightException {
    Image first = makeMe.anImage().forNote(note).please();
    Image second = makeMe.anImage().forNote(note).please();

    controller.updateNoteContent(note, contentDto("just markdown"));

    assertThat(imageRepository.findById(first.getId()).isPresent(), equalTo(false));
    assertThat(imageRepository.findById(second.getId()).isPresent(), equalTo(false));
  }

  @Test
  void skipsOrphanCleanupWhenImageScalarIsNotCanonicalAttachmentPath()
      throws UnexpectedNoAccessRightException {
    Image first = makeMe.anImage().forNote(note).please();
    Image second = makeMe.anImage().forNote(note).please();

    controller.updateNoteContent(
        note, contentDto("---\nimage: https://example.com/a.png\n---\nbody"));

    assertThat(imageRepository.findById(first.getId()).isPresent(), equalTo(true));
    assertThat(imageRepository.findById(second.getId()).isPresent(), equalTo(true));
  }

  @Test
  void storesFullEncodedPropertyWikiTokenWhenExactPropertyExists()
      throws UnexpectedNoAccessRightException {
    Note moon = noteWithExactProperty("Moon", "a part of");
    Note carrier = makeMe.aNote().underSameNotebookAs(moon).please();

    NoteRealm response =
        controller.updateNoteContent(carrier, contentDto("[[Moon#prop:a%20part%20of]]"));

    assertThat(response.getWikiTitles(), hasSize(1));
    WikiTitle wt = response.getWikiTitles().getFirst();
    assertThat(wt.getLinkText(), equalTo("Moon#prop:a%20part%20of"));
    assertThat(wt.getTargetToken(), equalTo("Moon#prop:a%20part%20of"));
    assertThat(wt.getNoteId(), equalTo(moon.getId()));

    List<NoteWikiTitleCache> rows =
        noteWikiTitleCacheRepository.findByNote_IdOrderByIdAsc(carrier.getId());
    assertThat(rows, hasSize(1));
    assertThat(rows.getFirst().getLinkText(), equalTo("Moon#prop:a%20part%20of"));
  }

  @Test
  void storesFullEncodedPropertyPathMarkdownTokenWhenExactPropertyExists()
      throws UnexpectedNoAccessRightException {
    Note moon = noteWithExactProperty("Moon", "a part of");
    Note carrier = makeMe.aNote().underSameNotebookAs(moon).please();
    String authored = "[a part of](/Moon.md#prop:a%20part%20of)";

    WikiTitle wt =
        controller.updateNoteContent(carrier, contentDto(authored)).getWikiTitles().getFirst();

    assertThat(wt.getLinkText(), equalTo(authored));
    assertThat(wt.getTargetToken(), equalTo("/Moon.md#prop:a%20part%20of"));
    assertThat(wt.getNoteId(), equalTo(moon.getId()));
  }

  @Test
  void omitsPropertyWikiTokenWhenExactPropertyDoesNotExist()
      throws UnexpectedNoAccessRightException {
    Note moon = makeMe.aNote().title("Moon").notebookOwnedBy(currentUser.getUser()).please();
    Note carrier = makeMe.aNote().underSameNotebookAs(moon).please();

    NoteRealm response =
        controller.updateNoteContent(carrier, contentDto("[[Moon#prop:a%20part%20of]]"));

    assertThat(response.getWikiTitles(), empty());
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
        response.getWikiTitles().stream().map(WikiTitle::getLinkText).toList(),
        equalTo(List.of("Moon#prop:Name", "Moon#prop:name")));
    assertThat(
        noteWikiTitleCacheRepository.findByNote_IdOrderByIdAsc(carrier.getId()).stream()
            .map(NoteWikiTitleCache::getLinkText)
            .toList(),
        equalTo(List.of("Moon#prop:Name", "Moon#prop:name")));
  }
}
