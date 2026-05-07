package com.odde.doughnut.controllers;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.odde.doughnut.controllers.dto.NoteRealm;
import com.odde.doughnut.controllers.dto.NoteUpdateContentDTO;
import com.odde.doughnut.controllers.dto.NoteUpdateTitleDTO;
import com.odde.doughnut.controllers.dto.WikiTitle;
import com.odde.doughnut.entities.Note;
import com.odde.doughnut.entities.NoteWikiTitleCache;
import com.odde.doughnut.entities.repositories.NoteWikiTitleCacheRepository;
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

  Note note;

  @BeforeEach
  void setup() {
    currentUser.setUser(makeMe.aUser().please());
    note = makeMe.aNote("new").creatorAndOwner(currentUser.getUser()).please();
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
      note = makeMe.aNote("another").creatorAndOwner(makeMe.aUser().please()).please();
      assertThrows(
          UnexpectedNoAccessRightException.class,
          () -> controller.updateNoteTitle(note, noteUpdateTitleDTO));
    }
  }

  @Nested
  class updateNoteContentTest {
    NoteUpdateContentDTO noteUpdateContentDTO = new NoteUpdateContentDTO();

    @BeforeEach
    void setup() {
      noteUpdateContentDTO.setContent("new details");
    }

    @Test
    void shouldBeAbleToSaveNoteWhenValid() throws UnexpectedNoAccessRightException, IOException {
      NoteRealm response = controller.updateNoteContent(note, noteUpdateContentDTO);
      assertThat(response.getId(), equalTo(note.getId()));
      assertThat(response.getNote().getContent(), equalTo("new details"));
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
      Note root = makeMe.aNote().creatorAndOwner(currentUser.getUser()).please();
      Note onlyA = makeMe.aNote().title("OnlyA").underSameNotebookAs(root).please();
      makeMe.aNote().title("OnlyB").underSameNotebookAs(root).please();
      Note carrier = makeMe.aNote().underSameNotebookAs(root).please();

      noteUpdateContentDTO.setContent("[[OnlyA]]");
      NoteRealm response = controller.updateNoteContent(carrier, noteUpdateContentDTO);

      assertThat(response.getWikiTitles(), hasSize(1));
      WikiTitle wt = response.getWikiTitles().getFirst();
      assertThat(wt.getLinkText(), equalTo("OnlyA"));
      assertThat(wt.getNoteId(), equalTo(onlyA.getId()));

      List<NoteWikiTitleCache> rows =
          noteWikiTitleCacheRepository.findByNote_IdOrderByIdAsc(carrier.getId());
      assertThat(rows, hasSize(1));
      assertThat(rows.getFirst().getLinkText(), equalTo("OnlyA"));
      assertThat(rows.getFirst().getTargetNote().getId(), equalTo(onlyA.getId()));
    }

    @Test
    void replacingWikiLinkUpdatesNoteRealmWikiTitlesAndPersistedCache()
        throws UnexpectedNoAccessRightException {
      Note root = makeMe.aNote().creatorAndOwner(currentUser.getUser()).please();
      makeMe.aNote().title("OnlyA").underSameNotebookAs(root).please();
      Note onlyB = makeMe.aNote().title("OnlyB").underSameNotebookAs(root).please();
      Note carrier = makeMe.aNote().underSameNotebookAs(root).please();

      noteUpdateContentDTO.setContent("[[OnlyA]]");
      controller.updateNoteContent(carrier, noteUpdateContentDTO);

      noteUpdateContentDTO.setContent("[[OnlyB]]");
      NoteRealm response = controller.updateNoteContent(carrier, noteUpdateContentDTO);

      assertThat(response.getWikiTitles(), hasSize(1));
      WikiTitle wt = response.getWikiTitles().getFirst();
      assertThat(wt.getLinkText(), equalTo("OnlyB"));
      assertThat(wt.getNoteId(), equalTo(onlyB.getId()));

      List<NoteWikiTitleCache> rows =
          noteWikiTitleCacheRepository.findByNote_IdOrderByIdAsc(carrier.getId());
      assertThat(rows, hasSize(1));
      assertThat(rows.getFirst().getLinkText(), equalTo("OnlyB"));
      assertThat(rows.getFirst().getTargetNote().getId(), equalTo(onlyB.getId()));
    }

    @Test
    void clearsWikiTitleCacheWhenContentBecomeBlank() throws UnexpectedNoAccessRightException {
      Note root = makeMe.aNote().creatorAndOwner(currentUser.getUser()).please();
      makeMe.aNote().title("OnlyA").underSameNotebookAs(root).please();
      Note carrier = makeMe.aNote().underSameNotebookAs(root).please();

      noteUpdateContentDTO.setContent("[[OnlyA]]");
      controller.updateNoteContent(carrier, noteUpdateContentDTO);
      assertThat(
          noteWikiTitleCacheRepository.findByNote_IdOrderByIdAsc(carrier.getId()), hasSize(1));

      noteUpdateContentDTO.setContent("");
      NoteRealm response = controller.updateNoteContent(carrier, noteUpdateContentDTO);

      assertThat(response.getWikiTitles(), empty());
      assertThat(noteWikiTitleCacheRepository.findByNote_IdOrderByIdAsc(carrier.getId()), empty());
    }
  }
}
