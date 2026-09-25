package com.odde.donut.controllers;

import com.odde.donut.controllers.currentUser.CurrentUser;
import com.odde.donut.controllers.currentUser.ThreadLocalCurrentUser;
import com.odde.donut.controllers.dto.NoteTrashDTO;
import com.odde.donut.controllers.dto.NoteTrashReferenceHandling;
import com.odde.donut.controllers.dto.NoteTrashUndoDTO;
import com.odde.donut.controllers.dto.NoteUpdateTitleDTO;
import com.odde.donut.entities.Folder;
import com.odde.donut.entities.Note;
import com.odde.donut.services.AuthorizationService;
import com.odde.donut.services.GithubService;
import com.odde.donut.services.notebookAttachment.InMemoryNotebookAttachmentContent;
import com.odde.donut.services.notebookGit.SqlStatementCallLogDataSourceConfig;
import com.odde.donut.testability.MakeMe;
import com.odde.donut.testability.TestabilitySettings;
import org.junit.jupiter.api.AfterEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.convention.TestBean;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
@Import(SqlStatementCallLogDataSourceConfig.class)
public abstract class ControllerTestBase {
  @Autowired protected MakeMe makeMe;
  @Autowired protected AuthorizationService authorizationService;
  @Autowired protected TestabilitySettings testabilitySettings;

  @TestBean protected CurrentUser currentUser;
  @MockitoBean protected GithubService githubService;

  static CurrentUser currentUser() {
    return new ThreadLocalCurrentUser();
  }

  @AfterEach
  void cleanupSharedTestState() {
    testabilitySettings.timeTravelTo(null);
    testabilitySettings.setOpenAiTokenOverride(null);
    ((InMemoryNotebookAttachmentContent) makeMe.notebookAttachmentContent).clear();
  }

  /** See {@link MakeMe#authorReferencingContent(Note, String)}. */
  protected void authorReferencingContent(Note note, String content) {
    makeMe.authorReferencingContent(note, content);
  }

  protected NoteTrashDTO leaveDeadLinks() {
    NoteTrashDTO request = new NoteTrashDTO();
    request.setReferenceHandling(NoteTrashReferenceHandling.LEAVE_DEAD_LINKS);
    return request;
  }

  protected NoteTrashDTO removeFromProperties() {
    NoteTrashDTO request = new NoteTrashDTO();
    request.setReferenceHandling(NoteTrashReferenceHandling.REMOVE_FROM_PROPERTIES);
    return request;
  }

  protected NoteTrashUndoDTO undoTo(String title, Folder folder) {
    NoteTrashUndoDTO request = new NoteTrashUndoDTO();
    request.setPriorTitle(title);
    request.setPriorFolderId(folder == null ? null : folder.getId());
    return request;
  }

  protected NoteUpdateTitleDTO titleDto(String newTitle) {
    NoteUpdateTitleDTO dto = new NoteUpdateTitleDTO();
    dto.setNewTitle(newTitle);
    return dto;
  }
}
