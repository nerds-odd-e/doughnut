package com.odde.doughnut.models;

import com.odde.doughnut.controllers.dto.CircleForUserView;
import com.odde.doughnut.controllers.dto.NotebookViewedByUser;
import com.odde.doughnut.controllers.dto.NotebooksViewedByUser;
import com.odde.doughnut.controllers.dto.UserForOtherUserView;
import com.odde.doughnut.entities.Circle;
import com.odde.doughnut.entities.Notebook;
import com.odde.doughnut.entities.User;
import java.util.List;
import java.util.stream.Collectors;

public class JsonViewer {
  private User viewer;

  public JsonViewer(User viewer) {

    this.viewer = viewer;
  }

  public NotebookViewedByUser jsonNotebookViewedByUser(Notebook notebook) {
    NotebookViewedByUser notebookViewedByUser = new NotebookViewedByUser();
    notebookViewedByUser.setId(notebook.getId());
    notebookViewedByUser.setHeadNoteId(notebook.getHeadNote().getId());
    notebookViewedByUser.setHeadNote(notebook.getHeadNote());
    notebookViewedByUser.setSkipReviewEntirely(notebook.getSkipReviewEntirely());
    return notebookViewedByUser;
  }

  public NotebooksViewedByUser jsonNotebooksViewedByUser(List<Notebook> allNotebooks) {
    NotebooksViewedByUser notebooksViewedByUser = new NotebooksViewedByUser();
    notebooksViewedByUser.notebooks =
        allNotebooks.stream().map(this::jsonNotebookViewedByUser).collect(Collectors.toList());
    return notebooksViewedByUser;
  }

  public CircleForUserView jsonCircleForUserView(Circle circle) {
    CircleForUserView circleForUserView = new CircleForUserView();
    circleForUserView.setId(circle.getId());
    circleForUserView.setName(circle.getName());
    circleForUserView.setInvitationCode(circle.getInvitationCode());
    circleForUserView.setNotebooks(jsonNotebooksViewedByUser(circle.getOwnership().getNotebooks()));
    circleForUserView.setMembers(UserForOtherUserView.fromList(circle.getMembers()));
    return circleForUserView;
  }
}
