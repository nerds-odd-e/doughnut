package com.odde.doughnut.services;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.odde.doughnut.entities.Notebook;
import com.odde.doughnut.entities.NotebookGroup;
import com.odde.doughnut.entities.User;
import com.odde.doughnut.exceptions.UnexpectedNoAccessRightException;
import com.odde.doughnut.testability.MakeMe;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class NotebookGroupServiceTest {

  @Autowired MakeMe makeMe;
  @Autowired NotebookGroupService notebookGroupService;

  @Nested
  class CreateGroup {
    @Test
    void persistsGroupForOwnersOwnership() throws UnexpectedNoAccessRightException {
      User owner = makeMe.aUser().please();
      NotebookGroup group =
          notebookGroupService.createGroup(owner, owner.getOwnership(), "My group");
      makeMe.refresh(group);
      assertThat(group.getName(), equalTo("My group"));
      assertThat(group.getOwnership().getId(), equalTo(owner.getOwnership().getId()));
      assertThat(group.getCreatedAt(), notNullValue());
    }

    @Test
    void rejectsCreateWhenActorDoesNotOwnTargetOwnership() {
      User owner = makeMe.aUser().please();
      User other = makeMe.aUser().please();
      assertThrows(
          UnexpectedNoAccessRightException.class,
          () -> notebookGroupService.createGroup(other, owner.getOwnership(), "Stolen"));
    }
  }

  @Nested
  class AssignNotebookToGroup {
    @Test
    void persistsNotebookGroupLink() throws UnexpectedNoAccessRightException {
      User owner = makeMe.aUser().please();
      Notebook notebook = makeMe.aNotebook().creatorAndOwner(owner).please();
      NotebookGroup group = notebookGroupService.createGroup(owner, owner.getOwnership(), "G");
      notebookGroupService.assignNotebookToGroup(owner, notebook, group);
      makeMe.refresh(notebook);
      assertThat(notebook.getNotebookGroup().getId(), equalTo(group.getId()));
    }

    @Test
    void rejectsWhenNotebookAndGroupBelongToDifferentOwnerships()
        throws UnexpectedNoAccessRightException {
      User a = makeMe.aUser().please();
      User b = makeMe.aUser().please();
      Notebook notebook = makeMe.aNotebook().creatorAndOwner(a).please();
      NotebookGroup group = notebookGroupService.createGroup(b, b.getOwnership(), "Gb");
      assertThrows(
          UnexpectedNoAccessRightException.class,
          () -> notebookGroupService.assignNotebookToGroup(a, notebook, group));
    }

    @Test
    void rejectsWhenActorDoesNotOwnGroup() throws UnexpectedNoAccessRightException {
      User owner = makeMe.aUser().please();
      User other = makeMe.aUser().please();
      Notebook notebook = makeMe.aNotebook().creatorAndOwner(owner).please();
      NotebookGroup group = notebookGroupService.createGroup(owner, owner.getOwnership(), "G");
      assertThrows(
          UnexpectedNoAccessRightException.class,
          () -> notebookGroupService.assignNotebookToGroup(other, notebook, group));
    }

    @Test
    void assignWorksWhenGroupPersistedViaMakeMe() throws UnexpectedNoAccessRightException {
      User owner = makeMe.aUser().please();
      NotebookGroup group =
          makeMe.aNotebookGroup().ownership(owner.getOwnership()).name("From builder").please();
      Notebook notebook = makeMe.aNotebook().creatorAndOwner(owner).please();
      notebookGroupService.assignNotebookToGroup(owner, notebook, group);
      makeMe.refresh(notebook);
      assertThat(notebook.getNotebookGroup().getId(), equalTo(group.getId()));
    }

    @Test
    void reassigningReplacesPreviousGroup() throws UnexpectedNoAccessRightException {
      User owner = makeMe.aUser().please();
      Notebook notebook = makeMe.aNotebook().creatorAndOwner(owner).please();
      NotebookGroup g1 = notebookGroupService.createGroup(owner, owner.getOwnership(), "G1");
      NotebookGroup g2 = notebookGroupService.createGroup(owner, owner.getOwnership(), "G2");
      notebookGroupService.assignNotebookToGroup(owner, notebook, g1);
      notebookGroupService.assignNotebookToGroup(owner, notebook, g2);
      makeMe.refresh(notebook);
      assertThat(notebook.getNotebookGroup().getId(), equalTo(g2.getId()));
    }
  }

  @Nested
  class ClearNotebookGroup {
    @Test
    void clearsNotebookGroupId() throws UnexpectedNoAccessRightException {
      User owner = makeMe.aUser().please();
      Notebook notebook = makeMe.aNotebook().creatorAndOwner(owner).please();
      NotebookGroup group = notebookGroupService.createGroup(owner, owner.getOwnership(), "G");
      notebookGroupService.assignNotebookToGroup(owner, notebook, group);
      notebookGroupService.clearNotebookGroup(owner, notebook);
      makeMe.refresh(notebook);
      assertThat(notebook.getNotebookGroup(), nullValue());
    }

    @Test
    void rejectsWhenActorDoesNotOwnNotebook() throws UnexpectedNoAccessRightException {
      User owner = makeMe.aUser().please();
      User other = makeMe.aUser().please();
      Notebook notebook = makeMe.aNotebook().creatorAndOwner(owner).please();
      assertThrows(
          UnexpectedNoAccessRightException.class,
          () -> notebookGroupService.clearNotebookGroup(other, notebook));
    }
  }
}
