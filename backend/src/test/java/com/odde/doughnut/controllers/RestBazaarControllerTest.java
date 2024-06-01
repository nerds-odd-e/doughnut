package com.odde.doughnut.controllers;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.odde.doughnut.controllers.dto.NotebooksViewedByUser;
import com.odde.doughnut.entities.Note;
import com.odde.doughnut.entities.Notebook;
import com.odde.doughnut.exceptions.UnexpectedNoAccessRightException;
import com.odde.doughnut.models.UserModel;
import com.odde.doughnut.testability.MakeMe;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class RestBazaarControllerTest {
  @Autowired private MakeMe makeMe;
  private UserModel adminUser;
  private UserModel notebookOwner;
  private RestBazaarController controller;
  private Note topNote;
  private Notebook notebook;

  @BeforeEach
  void setup() {
    adminUser = makeMe.anAdmin().toModelPlease();
    notebookOwner = makeMe.aUser().toModelPlease();
    topNote = makeMe.aNote().creatorAndOwner(notebookOwner).please();
    notebook = topNote.getNotebook();
    makeMe.aBazaarNodebook(notebook).please();
    controller = new RestBazaarController(makeMe.modelFactoryService, adminUser);
  }

  @Nested
  class RemoveFromBazaar {
    @Test
    void otherPeopleCannot() {
      controller =
          new RestBazaarController(makeMe.modelFactoryService, makeMe.aUser().toModelPlease());
      assertThrows(
          UnexpectedNoAccessRightException.class, () -> controller.removeFromBazaar(notebook));
      assertThat(makeMe.modelFactoryService.toBazaarModel().getAllNotebooks(), hasItem(notebook));
    }

    @Test
    void notebookOwnerCan() throws UnexpectedNoAccessRightException {
      controller = new RestBazaarController(makeMe.modelFactoryService, notebookOwner);
      controller.removeFromBazaar(notebook);
    }

    @Test
    void removeFromBazaarSuccessfully() throws UnexpectedNoAccessRightException {
      controller.removeFromBazaar(notebook);
      assertThat(
          makeMe.modelFactoryService.toBazaarModel().getAllNotebooks(), not(hasItem(notebook)));
    }

    @Test
    void returnCurrentBazaarNotes() throws UnexpectedNoAccessRightException {
      NotebooksViewedByUser notebooksViewedByUser = controller.removeFromBazaar(notebook);
      assertThat(notebooksViewedByUser.notebooks, hasSize(0));
    }
  }
}
