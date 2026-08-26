package com.odde.donut.controllers;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.odde.donut.entities.BazaarNotebook;
import com.odde.donut.entities.Notebook;
import com.odde.donut.entities.User;
import com.odde.donut.exceptions.UnexpectedNoAccessRightException;
import com.odde.donut.services.BazaarService;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

class BazaarControllerTest extends ControllerTestBase {
  @Autowired private BazaarService bazaarService;
  @Autowired BazaarController controller;

  private Notebook notebook;
  private BazaarNotebook bazaarNotebook;
  private User notebookOwnerUser;

  @BeforeEach
  void setup() {
    currentUser.setUser(makeMe.anAdmin().please());
    notebookOwnerUser = makeMe.aUser().please();
    notebook = makeMe.aNote().notebookOwnedBy(notebookOwnerUser).please().getNotebook();
    bazaarNotebook = makeMe.aBazaarNotebook(notebook).please();
  }

  @Nested
  class RemoveFromBazaar {
    @Test
    void nonOwnerDenied() {
      currentUser.setUser(makeMe.aUser().please());
      assertThrows(
          UnexpectedNoAccessRightException.class,
          () -> controller.removeFromBazaar(bazaarNotebook));
      assertThat(getAllBazaarNotebooks(), hasItem(notebook));
    }

    @Test
    void notebookOwnerCanRemove() throws UnexpectedNoAccessRightException {
      currentUser.setUser(notebookOwnerUser);
      assertThat(controller.removeFromBazaar(bazaarNotebook), empty());
    }

    @Test
    void adminRemovesAndReturnsEmptyList() throws UnexpectedNoAccessRightException {
      assertThat(controller.removeFromBazaar(bazaarNotebook), empty());
      assertThat(getAllBazaarNotebooks(), not(hasItem(notebook)));
    }
  }

  private List<Notebook> getAllBazaarNotebooks() {
    return bazaarService.getAllBazaarNotebooks().stream().map(BazaarNotebook::getNotebook).toList();
  }
}
