package com.odde.doughnut.controllers;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.odde.doughnut.configs.ObjectMapperConfig;
import com.odde.doughnut.controllers.dto.ApiError;
import com.odde.doughnut.controllers.dto.NoteRealm;
import com.odde.doughnut.controllers.dto.NoteUpdateContentDTO;
import com.odde.doughnut.controllers.dto.NoteUpdateTitleDTO;
import com.odde.doughnut.controllers.dto.TitleRenameReferenceHandling;
import com.odde.doughnut.controllers.dto.WikiTitle;
import com.odde.doughnut.entities.*;
import com.odde.doughnut.entities.repositories.ImageRepository;
import com.odde.doughnut.entities.repositories.NoteWikiTitleCacheRepository;
import com.odde.doughnut.exceptions.ApiException;
import com.odde.doughnut.exceptions.UnexpectedNoAccessRightException;
import java.io.IOException;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

class TextContentControllerTests extends ControllerTestBase {
  @Autowired TextContentController controller;
  @Autowired NoteWikiTitleCacheRepository noteWikiTitleCacheRepository;
  @Autowired ImageRepository imageRepository;

  Note note;

  @BeforeEach
  void setup() {
    currentUser.setUser(makeMe.aUser().please());
    Notebook notebook = makeMe.aNotebook().creatorAndOwner(currentUser.getUser()).please();
    note = makeMe.aNote("new").notebook(notebook).please();
  }

  @Nested
  class updateNoteTopticTest {
    NoteUpdateTitleDTO noteUpdateTitleDTO = new NoteUpdateTitleDTO();

    @BeforeEach
    void setup() {
      noteUpdateTitleDTO.setNewTitle("new title");
    }

    @Test
    void shouldBeAbleToSaveNoteTitle() throws UnexpectedNoAccessRightException {
      NoteRealm response = controller.updateNoteTitle(note, noteUpdateTitleDTO);
      assertThat(response.getId(), equalTo(note.getId()));
      assertThat(response.getNote().getTitle(), equalTo("new title"));
    }

    @Test
    void shouldPersistTitleWithoutSurroundingCrLfFromJson() throws Exception {
      ObjectMapper objectMapper = new ObjectMapperConfig().objectMapper();
      NoteUpdateTitleDTO titleDto =
          objectMapper.readValue("{\"newTitle\": \"\\r\\nAfter\\r\\n\"}", NoteUpdateTitleDTO.class);
      NoteRealm response = controller.updateNoteTitle(note, titleDto);
      assertThat(response.getNote().getTitle(), equalTo("After"));
    }

    @Test
    void shouldPreserveRecallSettingsWhenUpdatingTitle() throws UnexpectedNoAccessRightException {
      // Set remember spelling to true
      note.getRecallSetting().setRememberSpelling(true);
      makeMe.refresh(note);

      // Update the title
      NoteRealm response = controller.updateNoteTitle(note, noteUpdateTitleDTO);

      // Verify recall settings are preserved
      makeMe.refresh(note);
      assertThat(note.getRecallSetting().getRememberSpelling(), equalTo(true));
    }

    @Test
    void shouldNotAllowOthersToChange() {
      Notebook otherNotebook = makeMe.aNotebook().creatorAndOwner(makeMe.aUser().please()).please();
      note = makeMe.aNote("another").notebook(otherNotebook).please();
      assertThrows(
          UnexpectedNoAccessRightException.class,
          () -> controller.updateNoteTitle(note, noteUpdateTitleDTO));
    }
  }

  @Nested
  class updateNoteTitleInboundWikiReferences {
    @Test
    void rejectsRenameWithoutReferenceHandlingWhenInboundWikiLinksExist()
        throws UnexpectedNoAccessRightException {
      Notebook notebook = makeMe.aNotebook().creatorAndOwner(currentUser.getUser()).please();
      Note target = makeMe.aNote().title("TargetTitle").notebook(notebook).please();
      Note carrier = makeMe.aNote().notebook(notebook).please();

      NoteUpdateContentDTO contentDto = new NoteUpdateContentDTO();
      contentDto.setContent("[[TargetTitle]]");
      controller.updateNoteContent(carrier, contentDto);

      NoteUpdateTitleDTO titleDto = new NoteUpdateTitleDTO();
      titleDto.setNewTitle("RenamedTarget");

      ApiException thrown =
          assertThrows(ApiException.class, () -> controller.updateNoteTitle(target, titleDto));
      assertThat(thrown.getErrorBody().getErrorType(), equalTo(ApiError.ErrorType.BINDING_ERROR));
      assertThat(
          thrown.getErrorBody().getErrors().get("referenceHandling"), containsString("wiki"));

      makeMe.refresh(target);
      assertThat(target.getTitle(), equalTo("TargetTitle"));
    }

    @Test
    void allowsSameTitleWithoutReferenceHandlingWhenInboundWikiLinksExist()
        throws UnexpectedNoAccessRightException {
      Notebook notebook = makeMe.aNotebook().creatorAndOwner(currentUser.getUser()).please();
      Note target = makeMe.aNote().title("TargetTitle").notebook(notebook).please();
      Note carrier = makeMe.aNote().notebook(notebook).please();

      NoteUpdateContentDTO contentDto = new NoteUpdateContentDTO();
      contentDto.setContent("[[TargetTitle]]");
      controller.updateNoteContent(carrier, contentDto);

      NoteUpdateTitleDTO titleDto = new NoteUpdateTitleDTO();
      titleDto.setNewTitle("TargetTitle");

      NoteRealm response = controller.updateNoteTitle(target, titleDto);
      assertThat(response.getNote().getTitle(), equalTo("TargetTitle"));
    }

    @Test
    void allowsRenameWithExplicitReferenceHandlingWhenInboundWikiLinksExist()
        throws UnexpectedNoAccessRightException {
      Notebook notebook = makeMe.aNotebook().creatorAndOwner(currentUser.getUser()).please();
      Note target = makeMe.aNote().title("TargetTitle").notebook(notebook).please();
      Note carrier = makeMe.aNote().notebook(notebook).please();

      NoteUpdateContentDTO contentDto = new NoteUpdateContentDTO();
      contentDto.setContent("[[TargetTitle]]");
      controller.updateNoteContent(carrier, contentDto);

      NoteUpdateTitleDTO titleDto = new NoteUpdateTitleDTO();
      titleDto.setNewTitle("RenamedTarget");
      titleDto.setReferenceHandling(TitleRenameReferenceHandling.UPDATE_VISIBLE_TEXT);

      NoteRealm response = controller.updateNoteTitle(target, titleDto);
      assertThat(response.getNote().getTitle(), equalTo("RenamedTarget"));
      makeMe.refresh(carrier);
      assertThat(carrier.getContent(), equalTo("[[RenamedTarget]]"));
    }

    @Test
    void updateVisibleText_preservesExplicitDisplayTextAndRefreshesInboundMetadata()
        throws UnexpectedNoAccessRightException {
      Notebook notebook = makeMe.aNotebook().creatorAndOwner(currentUser.getUser()).please();
      Note target = makeMe.aNote().title("TargetTitle").notebook(notebook).please();
      Note carrier = makeMe.aNote().notebook(notebook).please();

      NoteUpdateContentDTO contentDto = new NoteUpdateContentDTO();
      contentDto.setContent("[[TargetTitle|custom label]]");
      controller.updateNoteContent(carrier, contentDto);

      NoteUpdateTitleDTO titleDto = new NoteUpdateTitleDTO();
      titleDto.setNewTitle("RenamedTarget");
      titleDto.setReferenceHandling(TitleRenameReferenceHandling.UPDATE_VISIBLE_TEXT);

      NoteRealm response = controller.updateNoteTitle(target, titleDto);
      assertThat(response.getReferences(), hasSize(1));
      assertThat(response.getReferences().getFirst().getId(), equalTo(carrier.getId()));

      makeMe.refresh(carrier);
      assertThat(carrier.getContent(), equalTo("[[RenamedTarget|custom label]]"));

      NoteWikiTitleCache row =
          noteWikiTitleCacheRepository.findByNote_IdOrderByIdAsc(carrier.getId()).getFirst();
      assertThat(row.getLinkText(), equalTo("RenamedTarget|custom label"));
      assertThat(row.getTargetNote().getId(), equalTo(target.getId()));
    }

    @Test
    void updateVisibleText_rewritesWikiLinkInsideYamlFrontmatter()
        throws UnexpectedNoAccessRightException {
      Notebook notebook = makeMe.aNotebook().creatorAndOwner(currentUser.getUser()).please();
      Note target = makeMe.aNote().title("Alpha").notebook(notebook).please();
      Note carrier = makeMe.aNote().notebook(notebook).please();

      NoteUpdateContentDTO contentDto = new NoteUpdateContentDTO();
      contentDto.setContent("---\nparent: \"[[Alpha]]\"\n---\n");
      controller.updateNoteContent(carrier, contentDto);

      NoteUpdateTitleDTO titleDto = new NoteUpdateTitleDTO();
      titleDto.setNewTitle("Beta");
      titleDto.setReferenceHandling(TitleRenameReferenceHandling.UPDATE_VISIBLE_TEXT);

      controller.updateNoteTitle(target, titleDto);

      makeMe.refresh(carrier);
      assertThat(carrier.getContent(), containsString("parent: \"[[Beta]]\""));
    }

    @Test
    void updateVisibleText_rewritesNotebookQualifiedWikiLink()
        throws UnexpectedNoAccessRightException {
      Notebook nb =
          makeMe.aNotebook().name("NbFixed").creatorAndOwner(currentUser.getUser()).please();
      Note target = makeMe.aNote().title("TargetTitle").notebook(nb).please();
      Note carrier = makeMe.aNote().notebook(nb).please();

      NoteUpdateContentDTO contentDto = new NoteUpdateContentDTO();
      contentDto.setContent("[[NbFixed:TargetTitle]]");
      controller.updateNoteContent(carrier, contentDto);

      NoteUpdateTitleDTO titleDto = new NoteUpdateTitleDTO();
      titleDto.setNewTitle("RenamedTarget");
      titleDto.setReferenceHandling(TitleRenameReferenceHandling.UPDATE_VISIBLE_TEXT);

      controller.updateNoteTitle(target, titleDto);

      makeMe.refresh(carrier);
      assertThat(carrier.getContent(), equalTo("[[NbFixed:RenamedTarget]]"));
    }

    @Test
    void keepVisibleText_plainWikiLinkBecomesDisplayLinkAndRefreshesCache()
        throws UnexpectedNoAccessRightException {
      Notebook notebook = makeMe.aNotebook().creatorAndOwner(currentUser.getUser()).please();
      Note target = makeMe.aNote().title("TargetTitle").notebook(notebook).please();
      Note carrier = makeMe.aNote().notebook(notebook).please();

      NoteUpdateContentDTO contentDto = new NoteUpdateContentDTO();
      contentDto.setContent("[[TargetTitle]]");
      controller.updateNoteContent(carrier, contentDto);

      NoteUpdateTitleDTO titleDto = new NoteUpdateTitleDTO();
      titleDto.setNewTitle("RenamedTarget");
      titleDto.setReferenceHandling(TitleRenameReferenceHandling.KEEP_VISIBLE_TEXT);

      NoteRealm response = controller.updateNoteTitle(target, titleDto);
      assertThat(response.getNote().getTitle(), equalTo("RenamedTarget"));
      assertThat(response.getReferences(), hasSize(1));

      makeMe.refresh(carrier);
      assertThat(carrier.getContent(), equalTo("[[RenamedTarget|TargetTitle]]"));
      NoteWikiTitleCache row =
          noteWikiTitleCacheRepository.findByNote_IdOrderByIdAsc(carrier.getId()).getFirst();
      assertThat(row.getLinkText(), equalTo("RenamedTarget|TargetTitle"));
      assertThat(row.getTargetNote().getId(), equalTo(target.getId()));
    }

    @Test
    void keepVisibleText_preservesExplicitDisplayWhileRetargetingTitle()
        throws UnexpectedNoAccessRightException {
      Notebook notebook = makeMe.aNotebook().creatorAndOwner(currentUser.getUser()).please();
      Note target = makeMe.aNote().title("TargetTitle").notebook(notebook).please();
      Note carrier = makeMe.aNote().notebook(notebook).please();

      NoteUpdateContentDTO contentDto = new NoteUpdateContentDTO();
      contentDto.setContent("[[TargetTitle|custom text]]");
      controller.updateNoteContent(carrier, contentDto);

      NoteUpdateTitleDTO titleDto = new NoteUpdateTitleDTO();
      titleDto.setNewTitle("RenamedTarget");
      titleDto.setReferenceHandling(TitleRenameReferenceHandling.KEEP_VISIBLE_TEXT);

      controller.updateNoteTitle(target, titleDto);

      makeMe.refresh(carrier);
      assertThat(carrier.getContent(), equalTo("[[RenamedTarget|custom text]]"));
    }
  }

  @Nested
  class updateNoteContentTest {
    NoteUpdateContentDTO noteUpdateContentDTO = new NoteUpdateContentDTO();

    @BeforeEach
    void setup() {
      noteUpdateContentDTO.setContent("new content");
    }

    @Test
    void shouldBeAbleToSaveNoteWhenValid() throws UnexpectedNoAccessRightException, IOException {
      NoteRealm response = controller.updateNoteContent(note, noteUpdateContentDTO);
      assertThat(response.getId(), equalTo(note.getId()));
      assertThat(response.getNote().getContent(), equalTo("new content"));
    }

    @Test
    void preservesLeadingYamlFrontmatterInContent() throws UnexpectedNoAccessRightException {
      String contentWithFrontmatter =
          """
          ---
          key_one: alpha
          key_two: beta
          ---

          # Body heading

          Paragraph content.
          """;
      noteUpdateContentDTO.setContent(contentWithFrontmatter);

      NoteRealm response = controller.updateNoteContent(note, noteUpdateContentDTO);

      assertThat(response.getNote().getContent(), equalTo(contentWithFrontmatter));
      makeMe.refresh(note);
      assertThat(note.getContent(), equalTo(contentWithFrontmatter));
    }

    @Test
    void refreshesWikiTitleCacheWhenContentContainResolvedWikiLink()
        throws UnexpectedNoAccessRightException {
      Notebook notebook = makeMe.aNotebook().creatorAndOwner(currentUser.getUser()).please();
      Note onlyA = makeMe.aNote().title("OnlyA").notebook(notebook).please();
      Note onlyB = makeMe.aNote().title("OnlyB").notebook(notebook).please();
      Note carrier = makeMe.aNote().notebook(notebook).please();

      noteUpdateContentDTO.setContent("[[OnlyA]]");
      controller.updateNoteContent(carrier, noteUpdateContentDTO);

      noteUpdateContentDTO.setContent("[[OnlyB]]");
      NoteRealm response = controller.updateNoteContent(carrier, noteUpdateContentDTO);

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
      Notebook notebook = makeMe.aNotebook().creatorAndOwner(currentUser.getUser()).please();
      Note onlyA = makeMe.aNote().title("OnlyA").notebook(notebook).please();
      Note carrier = makeMe.aNote().notebook(notebook).please();

      noteUpdateContentDTO.setContent("[[OnlyA|alias label]]");
      NoteRealm response = controller.updateNoteContent(carrier, noteUpdateContentDTO);

      assertThat(response.getWikiTitles(), hasSize(1));
      WikiTitle wt = response.getWikiTitles().getFirst();
      assertThat(wt.getLinkText(), equalTo("OnlyA|alias label"));
      assertThat(wt.getTargetToken(), equalTo("OnlyA"));
      assertThat(wt.getDisplayText(), equalTo("alias label"));
      assertThat(wt.getNoteId(), equalTo(onlyA.getId()));

      List<NoteWikiTitleCache> rows =
          noteWikiTitleCacheRepository.findByNote_IdOrderByIdAsc(carrier.getId());
      assertThat(rows, hasSize(1));
      assertThat(rows.getFirst().getLinkText(), equalTo("OnlyA|alias label"));
      assertThat(rows.getFirst().getTargetNote().getId(), equalTo(onlyA.getId()));
    }

    @Test
    void clearsWikiTitleCacheWhenContentBecomeBlank() throws UnexpectedNoAccessRightException {
      Notebook notebook = makeMe.aNotebook().creatorAndOwner(currentUser.getUser()).please();
      makeMe.aNote().title("OnlyA").notebook(notebook).please();
      Note carrier = makeMe.aNote().notebook(notebook).please();

      noteUpdateContentDTO.setContent("[[OnlyA]]");
      controller.updateNoteContent(carrier, noteUpdateContentDTO);
      assertThat(
          noteWikiTitleCacheRepository.findByNote_IdOrderByIdAsc(carrier.getId()), hasSize(1));

      noteUpdateContentDTO.setContent("");
      NoteRealm response = controller.updateNoteContent(carrier, noteUpdateContentDTO);

      assertThat(response.getWikiTitles(), empty());
      assertThat(noteWikiTitleCacheRepository.findByNote_IdOrderByIdAsc(carrier.getId()), empty());
    }

    @Test
    void deletesOrphanImagesWhenContentReferencesSingleAttachmentPath()
        throws UnexpectedNoAccessRightException {
      Image kept = makeMe.anImage().please();
      kept.setNote(note);
      kept = imageRepository.save(kept);
      Image orphan = makeMe.anImage().please();
      orphan.setNote(note);
      orphan = imageRepository.save(orphan);

      noteUpdateContentDTO.setContent(
          "---\nimage: /attachments/images/" + kept.getId() + "/" + kept.getName() + "\n---\nbody");

      controller.updateNoteContent(note, noteUpdateContentDTO);

      assertThat(imageRepository.findById(kept.getId()).isPresent(), equalTo(true));
      assertThat(imageRepository.findById(orphan.getId()).isPresent(), equalTo(false));
    }

    @Test
    void deletesAllNoteImagesWhenFrontmatterHasNoImageScalar()
        throws UnexpectedNoAccessRightException {
      Image first = makeMe.anImage().please();
      first.setNote(note);
      first = imageRepository.save(first);
      Image second = makeMe.anImage().please();
      second.setNote(note);
      second = imageRepository.save(second);

      noteUpdateContentDTO.setContent("just markdown");

      controller.updateNoteContent(note, noteUpdateContentDTO);

      assertThat(imageRepository.findById(first.getId()).isPresent(), equalTo(false));
      assertThat(imageRepository.findById(second.getId()).isPresent(), equalTo(false));
    }

    @Test
    void skipsOrphanCleanupWhenImageScalarIsNotCanonicalAttachmentPath()
        throws UnexpectedNoAccessRightException {
      Image first = makeMe.anImage().please();
      first.setNote(note);
      first = imageRepository.save(first);
      Image second = makeMe.anImage().please();
      second.setNote(note);
      second = imageRepository.save(second);

      noteUpdateContentDTO.setContent("---\nimage: https://example.com/a.png\n---\nbody");

      controller.updateNoteContent(note, noteUpdateContentDTO);

      assertThat(imageRepository.findById(first.getId()).isPresent(), equalTo(true));
      assertThat(imageRepository.findById(second.getId()).isPresent(), equalTo(true));
    }
  }
}
