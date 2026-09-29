package com.odde.donut.controllers;

import com.odde.donut.controllers.dto.NoteTrashDTO;
import com.odde.donut.controllers.dto.NoteTrashReferenceHandling;
import com.odde.donut.controllers.dto.NoteTrashUndoDTO;
import com.odde.donut.controllers.dto.NoteUpdateTitleDTO;
import com.odde.donut.controllers.dto.RecallHistoryItem;
import com.odde.donut.entities.Folder;
import com.odde.donut.entities.MemoryTracker;
import com.odde.donut.entities.RecallLog;
import com.odde.donut.exceptions.UnexpectedNoAccessRightException;
import com.odde.donut.testability.SpringTestBase;
import java.util.List;
import java.util.Objects;
import org.springframework.beans.factory.annotation.Autowired;

public abstract class ControllerTestBase extends SpringTestBase {
  @Autowired private MemoryTrackerController recallHistoryController;

  protected List<RecallLog> recallLogsOf(MemoryTracker tracker)
      throws UnexpectedNoAccessRightException {
    return recallHistoryController.getRecallHistory(tracker).stream()
        .map(RecallHistoryItem::getRecallLog)
        .filter(Objects::nonNull)
        .toList();
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
