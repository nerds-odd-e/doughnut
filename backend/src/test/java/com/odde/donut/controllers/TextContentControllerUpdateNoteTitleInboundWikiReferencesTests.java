package com.odde.donut.controllers;

import static com.odde.donut.entities.repositories.AuthoredNoteReferenceRowTestSupport.rowsFor;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.odde.donut.controllers.dto.ApiError;
import com.odde.donut.controllers.dto.NoteRealm;
import com.odde.donut.controllers.dto.NoteUpdateTitleDTO;
import com.odde.donut.controllers.dto.TitleRenameReferenceHandling;
import com.odde.donut.entities.AuthoredNoteReferenceRow;
import com.odde.donut.entities.Note;
import com.odde.donut.entities.Notebook;
import com.odde.donut.exceptions.ApiException;
import com.odde.donut.exceptions.UnexpectedNoAccessRightException;
import jakarta.persistence.EntityManager;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

class TextContentControllerUpdateNoteTitleInboundWikiReferencesTests
    extends TextContentControllerTestBase {
  @Autowired EntityManager entityManager;

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

    List<AuthoredNoteReferenceRow> rows = rowsFor(entityManager, inbound.carrier());
    assertThat(rows, hasSize(1));
    assertThat(rows.getFirst().getAuthoredLink(), equalTo("RenamedTarget|custom label"));
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
    List<AuthoredNoteReferenceRow> rows = rowsFor(entityManager, inbound.carrier());
    assertThat(rows, hasSize(1));
    assertThat(rows.getFirst().getAuthoredLink(), equalTo("RenamedTarget|TargetTitle"));
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

  @Test
  void keepVisibleText_preservesEncodedPropertySuffixAndAuthoredDisplay()
      throws UnexpectedNoAccessRightException {
    Note target = noteWithExactProperty("Moon", "a part of");
    Note carrier = makeMe.aNote().underSameNotebookAs(target).please();
    controller.updateNoteContent(carrier, contentDto("[[Moon#prop:a%20part%20of|the moon]]"));

    NoteUpdateTitleDTO titleDto = titleDto("Luna");
    titleDto.setReferenceHandling(TitleRenameReferenceHandling.KEEP_VISIBLE_TEXT);

    controller.updateNoteTitle(target, titleDto);

    makeMe.refresh(carrier);
    assertThat(carrier.getContent(), containsString("[[Luna#prop:a%20part%20of|the moon]]"));
  }

  @Test
  void updateVisibleText_keepsFolderPathPrefixOnWikiLink() throws UnexpectedNoAccessRightException {
    InboundWiki inbound = folderPathInboundWiki("[[Folder/Old]]");

    NoteUpdateTitleDTO titleDto = titleDto("New");
    titleDto.setReferenceHandling(TitleRenameReferenceHandling.UPDATE_VISIBLE_TEXT);

    controller.updateNoteTitle(inbound.target(), titleDto);

    makeMe.refresh(inbound.carrier());
    assertThat(inbound.carrier().getContent(), containsString("[[Folder/New]]"));
  }

  @Test
  void keepVisibleText_keepsFolderPathPrefixOnWikiLink() throws UnexpectedNoAccessRightException {
    InboundWiki inbound = folderPathInboundWiki("[[Folder/Old]]");

    NoteUpdateTitleDTO titleDto = titleDto("New");
    titleDto.setReferenceHandling(TitleRenameReferenceHandling.KEEP_VISIBLE_TEXT);

    controller.updateNoteTitle(inbound.target(), titleDto);

    makeMe.refresh(inbound.carrier());
    assertThat(inbound.carrier().getContent(), containsString("[[Folder/New|Folder/Old]]"));
  }
}
