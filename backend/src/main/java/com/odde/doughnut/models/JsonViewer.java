package com.odde.doughnut.models;

import com.odde.doughnut.controllers.dto.CircleForUserView;
import com.odde.doughnut.controllers.dto.NotebooksViewedByUser;
import com.odde.doughnut.controllers.dto.UserForOtherUserView;
import com.odde.doughnut.entities.Circle;
import com.odde.doughnut.entities.Notebook;
import java.util.List;

public class JsonViewer {

  public JsonViewer() {}

  public NotebooksViewedByUser jsonNotebooksViewedByUser(List<Notebook> allNotebooks) {
    NotebooksViewedByUser notebooksViewedByUser = new NotebooksViewedByUser();
    notebooksViewedByUser.notebooks = allNotebooks;
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
