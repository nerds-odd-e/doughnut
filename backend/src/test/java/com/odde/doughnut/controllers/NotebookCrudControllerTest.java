package com.odde.doughnut.controllers;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.fasterxml.jackson.databind.JsonNode;
import com.odde.doughnut.controllers.dto.NotebookClientView;
import com.odde.doughnut.controllers.dto.NotebookCreationRequest;
import com.odde.doughnut.controllers.dto.NotebookPageClientView;
import com.odde.doughnut.controllers.dto.NotebookUpdateRequest;
import com.odde.doughnut.entities.Note;
import com.odde.doughnut.entities.Notebook;
import com.odde.doughnut.entities.User;
import com.odde.doughnut.exceptions.UnexpectedNoAccessRightException;
import java.util.List;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.web.server.ResponseStatusException;

class NotebookCrudControllerTest extends NotebookControllerTestBase {

  @Nested
  class CreateNotebook {
    @Test
    void returnsNotebookIdAndDoesNotCreateNotes() throws UnexpectedNoAccessRightException {
      NotebookCreationRequest noteCreation = new NotebookCreationRequest();
      noteCreation.setNewTitle("My Notebook Title");
      NotebookClientView response = controller.createNotebook(noteCreation);
      assertThat(response.notebook().getId(), notNullValue());
      notebookRepository.findById(response.notebook().getId()).orElseThrow();
      assertThat(
          noteRepository.findNotesInNotebookRootFolderScopeByNotebookId(
              response.notebook().getId()),
          empty());
    }

    @Test
    void persistsDescriptionOnCreate() throws UnexpectedNoAccessRightException {
      NotebookCreationRequest noteCreation = new NotebookCreationRequest();
      noteCreation.setNewTitle("Notebook With Blurb");
      noteCreation.setDescription("  Catalog blurb  ");
      NotebookClientView response = controller.createNotebook(noteCreation);
      assertThat(response.notebook().getId(), notNullValue());
      Notebook nb = notebookRepository.findById(response.notebook().getId()).orElseThrow();
      assertThat(nb.getDescription(), equalTo("Catalog blurb"));
    }

    @Test
    void leavesDescriptionNullWhenUnset() throws UnexpectedNoAccessRightException {
      NotebookCreationRequest noteCreation = new NotebookCreationRequest();
      noteCreation.setNewTitle("Notebook No Blurb");
      NotebookClientView response = controller.createNotebook(noteCreation);
      assertThat(response.notebook().getId(), notNullValue());
      Notebook nb = notebookRepository.findById(response.notebook().getId()).orElseThrow();
      assertThat(nb.getDescription(), nullValue());
    }
  }

  @Nested
  class NotebookApiSerialization {
    @Test
    void getNotebookJsonDoesNotExposeLegacyNotebookIdentityWireKeys() throws Exception {
      NotebookCreationRequest noteCreation = new NotebookCreationRequest();
      noteCreation.setNewTitle("API Shape NB");
      noteCreation.setDescription("Blurb");
      NotebookClientView response = controller.createNotebook(noteCreation);
      Notebook nb = notebookRepository.findById(response.notebook().getId()).orElseThrow();

      NotebookPageClientView wire = controller.get(nb);
      String json = objectMapper.writeValueAsString(wire);
      JsonNode tree = objectMapper.readTree(json);

      assertThat(tree.has("headNoteId"), is(false));
      JsonNode notebookPayload = tree.get("notebook");
      assertThat(notebookPayload.get("id").asInt(), equalTo(nb.getId()));
      assertThat(notebookPayload.get("name").asText(), equalTo("API Shape NB"));
      assertThat(notebookPayload.get("description").asText(), equalTo("Blurb"));
      assertThat(tree.get("readonly").asBoolean(), is(false));
    }
  }

  @Nested
  class GetNotebook {
    @Test
    void ownerGetsWritableNotebookClientView() throws UnexpectedNoAccessRightException {
      User owner = currentUser.getUser();
      Notebook nb = makeMe.aNotebook().creatorAndOwner(owner).please();
      NotebookPageClientView view = controller.get(nb);
      assertThat(view.notebook().getId(), equalTo(nb.getId()));
      assertThat(view.readonly(), is(false));
    }

    @Test
    void anonymousGetsReadonlyNotebookClientViewWhenNotebookInBazaar()
        throws UnexpectedNoAccessRightException {
      User owner = makeMe.aUser().please();
      Notebook nb = makeMe.aNotebook().creatorAndOwner(owner).please();
      makeMe.aBazaarNotebook(nb).please();
      currentUser.setUser(null);
      NotebookPageClientView view = controller.get(nb);
      assertThat(view.notebook().getId(), equalTo(nb.getId()));
      assertThat(view.readonly(), is(true));
    }

    @Test
    void anonymousDeniedWhenNotebookNotInBazaar() {
      User owner = makeMe.aUser().please();
      Notebook nb = makeMe.aNotebook().creatorAndOwner(owner).please();
      currentUser.setUser(null);
      assertThrows(ResponseStatusException.class, () -> controller.get(nb));
    }

    @Test
    void includesLandingNoteIndexIdWhenEligibleRootNoteExists()
        throws UnexpectedNoAccessRightException {
      User owner = currentUser.getUser();
      Notebook nb = makeMe.aNotebook().creatorAndOwner(owner).please();
      Note index = makeMe.aNote().creatorAndOwner(owner).inNotebook(nb).title("index").please();

      NotebookPageClientView view = controller.get(nb);

      assertThat(view.indexNoteId(), equalTo(index.getId()));
    }

    @Test
    void deniesLoggedInUserWithoutReadAccessToNotebook() {
      User owner = makeMe.aUser().please();
      Notebook nb = makeMe.aNotebook().creatorAndOwner(owner).please();
      currentUser.setUser(makeMe.aUser().please());
      assertThrows(UnexpectedNoAccessRightException.class, () -> controller.get(nb));
    }

    @Test
    void omitsLandingNoteIndexIdWhenNoEligibleIndexNoteYet()
        throws UnexpectedNoAccessRightException {
      User owner = currentUser.getUser();
      Notebook nb = makeMe.aNotebook().creatorAndOwner(owner).please();

      NotebookPageClientView view = controller.get(nb);

      assertThat(view.indexNoteId(), nullValue());
    }
  }

  @Nested
  class showNoteTest {
    @Test
    void whenNotLogin() {
      currentUser.setUser(null);
      assertThrows(ResponseStatusException.class, () -> controller.myNotebooks());
    }

    @Test
    void whenLoggedIn() {
      User user = new User();
      currentUser.setUser(user);
      List<Notebook> notebooks = currentUser.getUser().getOwnership().getNotebooks();
      assertEquals(
          notebooks,
          controller.myNotebooks().notebooks.stream()
              .map(com.odde.doughnut.controllers.dto.NotebookClientView::notebook)
              .toList());
    }
  }

  @Nested
  class updateNotebook {
    @Test
    void shouldNotBeAbleToUpdateNotebookThatBelongsToOtherUser() {
      User anotherUser = makeMe.aUser().please();
      Note note = makeMe.aNote().creatorAndOwner(anotherUser).please();
      assertThrows(
          UnexpectedNoAccessRightException.class,
          () -> controller.updateNotebook(note.getNotebook(), new NotebookUpdateRequest()));
    }

    @Test
    void shouldPersistDescriptionOnUpdate() throws UnexpectedNoAccessRightException {
      Note note = makeMe.aNote().creatorAndOwner(currentUser.getUser()).please();
      var request = new NotebookUpdateRequest();
      request.setNotebookSettings(copyNotebookSettings(note.getNotebook()));
      request.setDescription("Notebook blurb");
      controller.updateNotebook(note.getNotebook(), request);
      assertThat(note.getNotebook().getDescription(), equalTo("Notebook blurb"));
    }

    @Test
    void shouldClearDescriptionWhenEmptyString() throws UnexpectedNoAccessRightException {
      Note note = makeMe.aNote().creatorAndOwner(currentUser.getUser()).please();
      note.getNotebook().setDescription("was set");
      var setRequest = new NotebookUpdateRequest();
      setRequest.setNotebookSettings(copyNotebookSettings(note.getNotebook()));
      setRequest.setDescription("");
      controller.updateNotebook(note.getNotebook(), setRequest);
      assertThat(note.getNotebook().getDescription(), nullValue());
    }

    @Test
    void shouldLeaveDescriptionUnchangedWhenDescriptionOmitted()
        throws UnexpectedNoAccessRightException {
      Note note = makeMe.aNote().creatorAndOwner(currentUser.getUser()).please();
      note.getNotebook().setDescription("unchanged");
      var request = new NotebookUpdateRequest();
      request.setNotebookSettings(copyNotebookSettings(note.getNotebook()));
      controller.updateNotebook(note.getNotebook(), request);
      assertThat(note.getNotebook().getDescription(), equalTo("unchanged"));
    }

    @Test
    void shouldPersistNameOnUpdate() throws UnexpectedNoAccessRightException {
      Note note = makeMe.aNote().creatorAndOwner(currentUser.getUser()).please();
      note.getNotebook().setName("Old Title");
      var request = new NotebookUpdateRequest();
      request.setNotebookSettings(copyNotebookSettings(note.getNotebook()));
      request.setName("  New Title  ");
      controller.updateNotebook(note.getNotebook(), request);
      assertThat(note.getNotebook().getName(), equalTo("New Title"));
    }

    @Test
    void shouldRejectEmptyOrWhitespaceNameOnUpdate() {
      Note note = makeMe.aNote().creatorAndOwner(currentUser.getUser()).please();
      var request = new NotebookUpdateRequest();
      request.setNotebookSettings(copyNotebookSettings(note.getNotebook()));
      request.setName("   ");
      assertThrows(
          ResponseStatusException.class,
          () -> controller.updateNotebook(note.getNotebook(), request));
      request.setName("");
      assertThrows(
          ResponseStatusException.class,
          () -> controller.updateNotebook(note.getNotebook(), request));
    }

    @Test
    void shouldLeaveNameUnchangedWhenNameOmitted() throws UnexpectedNoAccessRightException {
      Note note = makeMe.aNote().creatorAndOwner(currentUser.getUser()).please();
      note.getNotebook().setName("Original Name");
      var request = new NotebookUpdateRequest();
      request.setNotebookSettings(copyNotebookSettings(note.getNotebook()));
      controller.updateNotebook(note.getNotebook(), request);
      assertThat(note.getNotebook().getName(), equalTo("Original Name"));
    }
  }
}
