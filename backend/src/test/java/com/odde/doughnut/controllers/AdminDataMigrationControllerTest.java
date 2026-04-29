package com.odde.doughnut.controllers;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.odde.doughnut.exceptions.UnexpectedNoAccessRightException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

class AdminDataMigrationControllerTest extends ControllerTestBase {

  @Autowired AdminDataMigrationController controller;

  @Test
  void adminCanRunDataMigrationStub() throws UnexpectedNoAccessRightException {
    currentUser.setUser(makeMe.anAdmin().please());

    ResponseEntity<Void> response = controller.runDataMigration();

    assertEquals(HttpStatus.OK, response.getStatusCode());
  }

  @Test
  void nonAdminCannotRunDataMigrationStub() {
    currentUser.setUser(makeMe.aUser().please());

    assertThrows(UnexpectedNoAccessRightException.class, () -> controller.runDataMigration());
  }
}
