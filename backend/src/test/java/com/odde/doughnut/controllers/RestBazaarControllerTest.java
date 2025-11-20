package com.odde.doughnut.controllers;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.odde.doughnut.controllers.currentUser.CurrentUser;
import com.odde.doughnut.entities.BazaarNotebook;
import com.odde.doughnut.entities.Note;
import com.odde.doughnut.entities.Notebook;
import com.odde.doughnut.entities.User;
import com.odde.doughnut.exceptions.UnexpectedNoAccessRightException;
import com.odde.doughnut.services.AuthorizationService;
import com.odde.doughnut.services.BazaarService;
import com.odde.doughnut.testability.MakeMe;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.TestBean;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class BazaarControllerTest {
  @Autowired private MakeMe makeMe;
  @Autowired private BazaarService bazaarService;

  @Autowired
  private com.odde.doughnut.entities.repositories.BazaarNotebookRepository bazaarNotebookRepository;

  @Autowired private AuthorizationService authorizationService;
  @TestBean private CurrentUser currentUser = new CurrentUser(null);
  private User adminUser;
  private User notebookOwner;
  private BazaarController controller;
  private Note topNote;
  private Notebook notebook;
  private BazaarNotebook bazaarNotebook;

  @BeforeEach
  void setup() {
    adminUser = makeMe.anAdmin().please();
    notebookOwner = makeMe.aUser().please();
    currentUser.setUser(adminUser);
    topNote = makeMe.aNote().creatorAndOwner(notebookOwner).please();
    notebook = topNote.getNotebook();
    bazaarNotebook = makeMe.aBazaarNotebook(notebook).please();
    controller = new BazaarController(bazaarService, authorizationService);
  }

  @Nested
  class RemoveFromBazaar {
    @Test
    void otherPeopleCannot() {
      currentUser.setUser(makeMe.aUser().please());
      controller = new BazaarController(bazaarService, authorizationService);
      assertThrows(
          UnexpectedNoAccessRightException.class,
          () -> controller.removeFromBazaar(bazaarNotebook));
      assertThat(getAllBazaarNotebooks(), hasItem(notebook));
    }

    @Test
    void notebookOwnerCan() throws UnexpectedNoAccessRightException {
      currentUser.setUser(notebookOwner);
      controller = new BazaarController(bazaarService, authorizationService);
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
