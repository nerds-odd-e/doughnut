package com.odde.doughnut.controllers;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.odde.doughnut.entities.BazaarNotebook;
import com.odde.doughnut.entities.Note;
import com.odde.doughnut.entities.Notebook;
import com.odde.doughnut.exceptions.UnexpectedNoAccessRightException;
import com.odde.doughnut.models.UserModel;
import com.odde.doughnut.testability.MakeMe;
import java.util.List;
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
class BazaarControllerTest {
  @Autowired private MakeMe makeMe;
  private UserModel adminUser;
  private UserModel notebookOwner;
  private BazaarController controller;
  private Note topNote;
  private Notebook notebook;
  private BazaarNotebook bazaarNotebook;

  @BeforeEach
  void setup() {
    adminUser = makeMe.anAdmin().toModelPlease();
    notebookOwner = makeMe.aUser().toModelPlease();
    topNote = makeMe.aNote().creatorAndOwner(notebookOwner).please();
    notebook = topNote.getNotebook();
    bazaarNotebook = makeMe.aBazaarNotebook(notebook).please();
    controller = new BazaarController(makeMe.modelFactoryService, adminUser);
  }

  @Nested
  class RemoveFromBazaar {
    @Test
    void otherPeopleCannot() {
      controller = new BazaarController(makeMe.modelFactoryService, makeMe.aUser().toModelPlease());
      assertThrows(
          UnexpectedNoAccessRightException.class,
          () -> controller.removeFromBazaar(bazaarNotebook));
      assertThat(getAllBazaarNotebooks(), hasItem(notebook));
    }

    @Test
    void notebookOwnerCan() throws UnexpectedNoAccessRightException {
      controller = new BazaarController(makeMe.modelFactoryService, notebookOwner);
      controller.removeFromBazaar(bazaarNotebook);
    }

    @Test
    void removeFromBazaarSuccessfully() throws UnexpectedNoAccessRightException {
      controller.removeFromBazaar(bazaarNotebook);
      assertThat(getAllBazaarNotebooks(), not(hasItem(notebook)));
    }

    @Test
    void returnCurrentBazaarNotes() throws UnexpectedNoAccessRightException {
      List<BazaarNotebook> notebooksViewedByUser = controller.removeFromBazaar(bazaarNotebook);
      assertThat(notebooksViewedByUser, hasSize(0));
    }
  }

  private List<Notebook> getAllBazaarNotebooks() {
    return makeMe.modelFactoryService.toBazaarModel().getAllBazaarNotebooks().stream()
        .map(BazaarNotebook::getNotebook)
        .toList();
  }
}
