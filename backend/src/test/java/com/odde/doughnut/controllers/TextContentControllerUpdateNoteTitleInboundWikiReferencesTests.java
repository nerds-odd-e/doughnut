package com.odde.doughnut.controllers;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.odde.doughnut.controllers.dto.ApiError;
import com.odde.doughnut.controllers.dto.NoteRealm;
import com.odde.doughnut.controllers.dto.NoteUpdateTitleDTO;
import com.odde.doughnut.controllers.dto.TitleRenameReferenceHandling;
import com.odde.doughnut.entities.Note;
import com.odde.doughnut.entities.NoteWikiTitleCache;
import com.odde.doughnut.entities.Notebook;
import com.odde.doughnut.entities.repositories.NoteWikiTitleCacheRepository;
import com.odde.doughnut.exceptions.ApiException;
import com.odde.doughnut.exceptions.UnexpectedNoAccessRightException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

class TextContentControllerUpdateNoteTitleInboundWikiReferencesTests
    extends TextContentControllerTestBase {
  @Autowired NoteWikiTitleCacheRepository noteWikiTitleCacheRepository;

  @Test
  void rejectsRenameWithoutReferenceHandlingWhenInboundWikiLinksExist()
      throws UnexpectedNoAccessRightException {
    InboundWiki inbound = noteWithInboundWiki("TargetTitle", "[[TargetTitle]]");

    ApiException thrown =
        assertThrows(
            ApiException.class,
            () -> controller.updateNoteTitle(inbound.target(), titleDto("RenamedTarget")));
    assertThat(thrown.getErrorBody().getErrorType(), equalTo(ApiError.ErrorType.BINDING_ERROR));
    assertThat(thrown.getErrorBody().getErrors().get("referenceHandling"), containsString("wiki"));
    makeMe.refresh(inbound.target());
    assertThat(inbound.target().getTitle(), equalTo("TargetTitle"));
  }

  @Test
  void allowsSameTitleWithoutReferenceHandlingWhenInboundWikiLinksExist()
      throws UnexpectedNoAccessRightException {
    InboundWiki inbound = noteWithInboundWiki("TargetTitle", "[[TargetTitle]]");

    assertThat(
        controller.updateNoteTitle(inbound.target(), titleDto("TargetTitle")).getNote().getTitle(),
        equalTo("TargetTitle"));
  }

  @Test
  void allowsRenameWithExplicitReferenceHandlingWhenInboundWikiLinksExist()
      throws UnexpectedNoAccessRightException {
    InboundWiki inbound = noteWithInboundWiki("TargetTitle", "[[TargetTitle]]");

    NoteUpdateTitleDTO titleDto = titleDto("RenamedTarget");
    titleDto.setReferenceHandling(TitleRenameReferenceHandling.UPDATE_VISIBLE_TEXT);

    NoteRealm response = controller.updateNoteTitle(inbound.target(), titleDto);
    assertThat(response.getNote().getTitle(), equalTo("RenamedTarget"));
    makeMe.refresh(inbound.carrier());
    assertThat(inbound.carrier().getContent(), containsString("[[RenamedTarget]]"));
  }

  @Test
  void updateVisibleText_preservesExplicitDisplayTextAndRefreshesInboundMetadata()
      throws UnexpectedNoAccessRightException {
    InboundWiki inbound = noteWithInboundWiki("TargetTitle", "[[TargetTitle|custom label]]");

    NoteUpdateTitleDTO titleDto = titleDto("RenamedTarget");
    titleDto.setReferenceHandling(TitleRenameReferenceHandling.UPDATE_VISIBLE_TEXT);

    NoteRealm response = controller.updateNoteTitle(inbound.target(), titleDto);
    assertThat(response.getReferences(), hasSize(1));
    assertThat(response.getReferences().getFirst().getId(), equalTo(inbound.carrier().getId()));

    makeMe.refresh(inbound.carrier());
    assertThat(inbound.carrier().getContent(), containsString("[[RenamedTarget|custom label]]"));

    NoteWikiTitleCache row =
        noteWikiTitleCacheRepository
            .findByNote_IdOrderByIdAsc(inbound.carrier().getId())
            .getFirst();
    assertThat(row.getLinkText(), equalTo("RenamedTarget|custom label"));
    assertThat(row.getTargetNote().getId(), equalTo(inbound.target().getId()));
  }

  @Test
  void updateVisibleText_rewritesWikiLinkInsideYamlFrontmatter()
      throws UnexpectedNoAccessRightException {
    InboundWiki inbound = noteWithInboundWiki("Alpha", "---\nparent: \"[[Alpha]]\"\n---\n");

    NoteUpdateTitleDTO titleDto = titleDto("Beta");
    titleDto.setReferenceHandling(TitleRenameReferenceHandling.UPDATE_VISIBLE_TEXT);

    controller.updateNoteTitle(inbound.target(), titleDto);

    makeMe.refresh(inbound.carrier());
    assertThat(inbound.carrier().getContent(), containsString("parent: \"[[Beta]]\""));
  }

  @Test
  void updateVisibleText_rewritesNotebookQualifiedWikiLink()
      throws UnexpectedNoAccessRightException {
    Notebook nb =
        makeMe.aNotebook().name("NbFixed").creatorAndOwner(currentUser.getUser()).please();
    Note target = makeMe.aNote().title("TargetTitle").notebook(nb).please();
    Note carrier = makeMe.aNote().notebook(nb).please();
    controller.updateNoteContent(carrier, contentDto("[[NbFixed:TargetTitle]]"));

    NoteUpdateTitleDTO titleDto = titleDto("RenamedTarget");
    titleDto.setReferenceHandling(TitleRenameReferenceHandling.UPDATE_VISIBLE_TEXT);

    controller.updateNoteTitle(target, titleDto);

    makeMe.refresh(carrier);
    assertThat(carrier.getContent(), containsString("[[NbFixed:RenamedTarget]]"));
  }

  @Test
  void keepVisibleText_plainWikiLinkBecomesDisplayLinkAndRefreshesCache()
      throws UnexpectedNoAccessRightException {
    InboundWiki inbound = noteWithInboundWiki("TargetTitle", "[[TargetTitle]]");

    NoteUpdateTitleDTO titleDto = titleDto("RenamedTarget");
    titleDto.setReferenceHandling(TitleRenameReferenceHandling.KEEP_VISIBLE_TEXT);

    NoteRealm response = controller.updateNoteTitle(inbound.target(), titleDto);
    assertThat(response.getNote().getTitle(), equalTo("RenamedTarget"));
    assertThat(response.getReferences(), hasSize(1));

    makeMe.refresh(inbound.carrier());
    assertThat(inbound.carrier().getContent(), containsString("[[RenamedTarget|TargetTitle]]"));
    NoteWikiTitleCache row =
        noteWikiTitleCacheRepository
            .findByNote_IdOrderByIdAsc(inbound.carrier().getId())
            .getFirst();
    assertThat(row.getLinkText(), equalTo("RenamedTarget|TargetTitle"));
    assertThat(row.getTargetNote().getId(), equalTo(inbound.target().getId()));
  }

  @Test
  void keepVisibleText_preservesExplicitDisplayWhileRetargetingTitle()
      throws UnexpectedNoAccessRightException {
    InboundWiki inbound = noteWithInboundWiki("TargetTitle", "[[TargetTitle|custom text]]");

    NoteUpdateTitleDTO titleDto = titleDto("RenamedTarget");
    titleDto.setReferenceHandling(TitleRenameReferenceHandling.KEEP_VISIBLE_TEXT);

    controller.updateNoteTitle(inbound.target(), titleDto);

    makeMe.refresh(inbound.carrier());
    assertThat(inbound.carrier().getContent(), containsString("[[RenamedTarget|custom text]]"));
  }
}
