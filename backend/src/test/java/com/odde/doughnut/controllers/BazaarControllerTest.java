package com.odde.doughnut.controllers;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.odde.doughnut.entities.BazaarNotebook;
import com.odde.doughnut.entities.Note;
import com.odde.doughnut.entities.Notebook;
import com.odde.doughnut.entities.User;
import com.odde.doughnut.exceptions.UnexpectedNoAccessRightException;
import com.odde.doughnut.services.BazaarService;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

class BazaarControllerTest extends ControllerTestBase {
  @Autowired private BazaarService bazaarService;

  @Autowired BazaarController controller;
  private Note topNote;
  private Notebook notebook;
  private BazaarNotebook bazaarNotebook;
  private User notebookOwnerUser;

  @BeforeEach
  void setup() {
    currentUser.setUser(makeMe.anAdmin().please());
    notebookOwnerUser = makeMe.aUser().please();
    topNote = makeMe.aNote().creatorAndOwner(notebookOwnerUser).please();
    notebook = topNote.getNotebook();
    bazaarNotebook = makeMe.aBazaarNotebook(notebook).please();
  }

  @Nested
  class RemoveFromBazaar {
    @Test
    void otherPeopleCannot() {
      currentUser.setUser(makeMe.aUser().please());
      assertThrows(
          UnexpectedNoAccessRightException.class,
          () -> controller.removeFromBazaar(bazaarNotebook));
      assertThat(getAllBazaarNotebooks(), hasItem(notebook));
    }

    @Test
    void notebookOwnerCan() throws UnexpectedNoAccessRightException {
      currentUser.setUser(notebookOwnerUser);
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
    return bazaarService.getAllBazaarNotebooks().stream().map(BazaarNotebook::getNotebook).toList();
  }
}
