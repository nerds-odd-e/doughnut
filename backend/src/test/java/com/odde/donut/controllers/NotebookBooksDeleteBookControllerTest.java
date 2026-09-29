package com.odde.donut.controllers;

import static org.junit.jupiter.api.Assertions.assertThrows;

import com.odde.donut.entities.Notebook;
import com.odde.donut.exceptions.UnexpectedNoAccessRightException;
import org.junit.jupiter.api.Test;
import org.springframework.web.server.ResponseStatusException;

class NotebookBooksDeleteBookControllerTest extends NotebookBooksControllerTestBase {

  @Test
  void returns404WhenNotebookHasNoBook() throws UnexpectedNoAccessRightException {
    Notebook nb = myNotebook();
    assertThrows(ResponseStatusException.class, () -> controller.deleteBook(nb));
  }

  @Test
  void rejectsUnauthorizedNotebook() {
    Notebook otherNb = otherUsersNotebook();
    assertThrows(UnexpectedNoAccessRightException.class, () -> controller.deleteBook(otherNb));
  }
}
