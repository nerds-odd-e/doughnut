package com.odde.doughnut.controllers;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.fasterxml.jackson.databind.JsonNode;
import com.odde.doughnut.controllers.dto.NoteUpdateContentDTO;
import com.odde.doughnut.controllers.dto.NotebookCreationRequest;
import com.odde.doughnut.controllers.dto.NotebookRealm;
import com.odde.doughnut.entities.Notebook;
import com.odde.doughnut.entities.NotebookGroup;
import com.odde.doughnut.entities.User;
import com.odde.doughnut.exceptions.UnexpectedNoAccessRightException;
import java.util.List;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

class NotebookCrudControllerTest extends NotebookControllerTestBase {

  private NotebookCreationRequest notebookCreate(String title) {
    NotebookCreationRequest req = new NotebookCreationRequest();
    req.setNewTitle(title);
    return req;
  }

  @Nested
  class CreateNotebook {
    @Test
    void returnsNotebookIdAndDoesNotCreateNotes() throws UnexpectedNoAccessRightException {
      NotebookRealm response = controller.createNotebook(notebookCreate("My Notebook Title"));
      assertThat(response.notebook().getId(), notNullValue());
      notebookRepository.findById(response.notebook().getId()).orElseThrow();
      assertThat(
          noteRepository.findNotesInNotebookRootFolderScopeByNotebookId(
              response.notebook().getId()),
          empty());
    }

    @Test
    void persistsDescriptionOnCreate() throws UnexpectedNoAccessRightException {
      NotebookCreationRequest noteCreation = notebookCreate("Notebook With Blurb");
      noteCreation.setDescription("  Catalog blurb  ");
      NotebookRealm response = controller.createNotebook(noteCreation);
      Notebook nb = notebookRepository.findById(response.notebook().getId()).orElseThrow();
      assertThat(nb.getDescription(), equalTo("Catalog blurb"));
    }

    @Test
    void leavesDescriptionNullWhenUnset() throws UnexpectedNoAccessRightException {
      NotebookRealm response = controller.createNotebook(notebookCreate("Notebook No Blurb"));
      Notebook nb = notebookRepository.findById(response.notebook().getId()).orElseThrow();
      assertThat(nb.getDescription(), nullValue());
    }

    @Test
    void assignsToGroupWhenNotebookGroupIdGiven() throws UnexpectedNoAccessRightException {
      User user = currentUser.getUser();
      NotebookGroup group =
          notebookGroupService.createGroup(user, user.getOwnership(), "Create Into");
      NotebookCreationRequest req = notebookCreate("Grouped On Create");
      req.setNotebookGroupId(group.getId());

      NotebookRealm response = controller.createNotebook(req);

      Notebook nb = notebookRepository.findById(response.notebook().getId()).orElseThrow();
      assertThat(nb.getNotebookGroup().getId(), equalTo(group.getId()));
    }

    @Test
    void rejectsUnknownNotebookGroupId() {
      NotebookCreationRequest req = notebookCreate("Missing Group");
      req.setNotebookGroupId(Integer.MAX_VALUE);
      ResponseStatusException ex =
          assertThrows(ResponseStatusException.class, () -> controller.createNotebook(req));
      assertThat(ex.getStatusCode(), equalTo(HttpStatus.NOT_FOUND));
    }

    @Test
    void rejectsGroupOwnedByAnotherUser() throws UnexpectedNoAccessRightException {
      User other = makeMe.aUser().please();
      NotebookGroup otherGroup =
          notebookGroupService.createGroup(other, other.getOwnership(), "Other");
      NotebookCreationRequest req = notebookCreate("Wrong Ownership Group");
      req.setNotebookGroupId(otherGroup.getId());
      assertThrows(UnexpectedNoAccessRightException.class, () -> controller.createNotebook(req));
    }
  }

  @Nested
  class NotebookApiSerialization {
    @Test
    void getNotebookJsonDoesNotExposeLegacyNotebookIdentityWireKeys() throws Exception {
      NotebookCreationRequest noteCreation = notebookCreate("API Shape NB");
      noteCreation.setDescription("Blurb");
      NotebookRealm response = controller.createNotebook(noteCreation);
      Notebook nb = notebookRepository.findById(response.notebook().getId()).orElseThrow();

      String json = objectMapper.writeValueAsString(controller.get(nb));
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
    void ownerGetsWritableNotebookRealm() throws UnexpectedNoAccessRightException {
      Notebook nb = ownedNotebook();
      NotebookRealm realm = controller.get(nb);
      assertThat(realm.notebook().getId(), equalTo(nb.getId()));
      assertThat(realm.readonly(), is(false));
    }

    @Test
    void anonymousGetsReadonlyNotebookRealmWhenNotebookInBazaar()
        throws UnexpectedNoAccessRightException {
      Notebook nb = makeMe.aNotebook().creatorAndOwner(makeMe.aUser().please()).please();
      makeMe.aBazaarNotebook(nb).please();
      currentUser.setUser(null);
      NotebookRealm realm = controller.get(nb);
      assertThat(realm.readonly(), is(true));
    }

    @Test
    void anonymousDeniedWhenNotebookNotInBazaar() {
      Notebook nb = makeMe.aNotebook().creatorAndOwner(makeMe.aUser().please()).please();
      currentUser.setUser(null);
      assertThrows(ResponseStatusException.class, () -> controller.get(nb));
    }

    @Test
    void deniesLoggedInUserWithoutReadAccessToNotebook() {
      Notebook nb = makeMe.aNotebook().creatorAndOwner(makeMe.aUser().please()).please();
      currentUser.setUser(makeMe.aUser().please());
      assertThrows(UnexpectedNoAccessRightException.class, () -> controller.get(nb));
    }

    @Test
    void exposesContainerReadmeContentWhenMigratedMarkdownExists()
        throws UnexpectedNoAccessRightException {
      Notebook nb =
          makeMe
              .aNotebook()
              .creatorAndOwner(currentUser.getUser())
              .readmeContent("---\ntitle_pattern: \"{{date}}\"\n---\n\nNotebook readme body")
              .please();

      assertThat(
          controller.get(nb).readmeContent(),
          equalTo("---\ntitle_pattern: \"{{date}}\"\n---\n\nNotebook readme body"));
    }

    @Test
    void omitsReadmeContentWhenNonePresent() throws UnexpectedNoAccessRightException {
      assertThat(controller.get(ownedNotebook()).readmeContent(), nullValue());
    }
  }

  @Nested
  class UpdateNotebookReadmeContent {
    @Test
    void updatesReadmeContentDirectly() throws UnexpectedNoAccessRightException {
      NoteUpdateContentDTO dto = new NoteUpdateContentDTO();
      dto.setContent("direct notebook readme content");

      NotebookRealm result = controller.updateNotebookReadmeContent(ownedNotebook(), dto);

      assertThat(result.readmeContent(), equalTo("direct notebook readme content"));
    }

    @Test
    void clearsReadmeContentWhenBlankContentGiven() throws UnexpectedNoAccessRightException {
      Notebook nb =
          makeMe
              .aNotebook()
              .creatorAndOwner(currentUser.getUser())
              .readmeContent("old content")
              .please();
      NoteUpdateContentDTO dto = new NoteUpdateContentDTO();
      dto.setContent("   ");

      assertThat(controller.updateNotebookReadmeContent(nb, dto).readmeContent(), nullValue());
    }

    @Test
    void requiresAuthorizationToUpdateReadmeContent() {
      Notebook nb = makeMe.aNotebook().creatorAndOwner(makeMe.aUser().please()).please();
      currentUser.setUser(makeMe.aUser().please());
      assertThrows(
          UnexpectedNoAccessRightException.class,
          () -> controller.updateNotebookReadmeContent(nb, new NoteUpdateContentDTO()));
    }
  }

  @Nested
  class MyNotebooks {
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
              .map(com.odde.doughnut.controllers.dto.NotebookRealm::notebook)
              .toList());
    }
  }
}
