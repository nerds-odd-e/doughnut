package com.odde.donut.controllers;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.odde.donut.controllers.dto.NoteUpdateTitleDTO;
import com.odde.donut.controllers.dto.TitleRenameReferenceHandling;
import com.odde.donut.entities.Folder;
import com.odde.donut.entities.Note;
import com.odde.donut.entities.Notebook;
import com.odde.donut.entities.User;
import com.odde.donut.services.notebookGit.AcceptedWebChangeService;
import com.odde.donut.testability.GitBundleTestReader.AcceptedHistory;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

class NotebookGitWebLinkingNotebookControllerTest extends NotebookGitWebContentControllerTestBase {
  @Autowired RelationController relationController;
  @Autowired AcceptedWebChangeService acceptedWebChangeService;

  @Test
  void aChangeToABoundNotebookOutsideTheLockedSetIsRefusedAndNothingIsCommitted() throws Exception {
    LinkingFixture f = seedForceLinkedFrom("See [[Science:Force]].");
    AcceptedHistory scienceBefore = acceptedHistory(f.science());

    ResponseStatusException refused =
        assertThrows(
            ResponseStatusException.class,
            () ->
                acceptedWebChangeService.apply(
                    Set.of(f.science().getId()),
                    () -> {
                      noteRepository.findById(f.force().getId()).orElseThrow().setContent("Mass");
                      noteRepository
                          .findById(f.ownReferrer().getId())
                          .orElseThrow()
                          .setContent("See [[Science:Load]].");
                      return null;
                    },
                    ignored -> "change",
                    Timestamp.from(Instant.now())));

    assertThat(refused.getStatusCode(), equalTo(HttpStatus.CONFLICT));
    assertThat(acceptedHistory(f.science()).commits(), equalTo(scienceBefore.commits()));
    assertThat(
        acceptedHistory(f.engineering()).commits(), equalTo(f.engineeringBefore().commits()));
  }

  @Test
  void renameCommitsTheRewrittenLinkInTheOwnLinkingNotebookButNotASubscribedOne() throws Exception {
    LinkingFixture f = seedForceLinkedFrom("See [[Science:Force]].");
    NoteUpdateTitleDTO rename = titleDto("Load");
    rename.setReferenceHandling(TitleRenameReferenceHandling.UPDATE_VISIBLE_TEXT);

    textContentController.updateNoteTitle(f.force(), rename);

    assertLinkingNotebookCommittedOnce(f, "See [[Science:Load]].");
    assertThat(contentOf(f.sharedReferrer()), equalTo("See [[Science:Force]]."));
  }

  @Test
  void moveWithinTheNotebookCommitsTheRewrittenLinkInTheLinkingNotebook() throws Exception {
    LinkingFixture f = seedForceLinkedFrom("physics", "See [[Science:physics/Force]].");
    Folder mechanics = makeMe.aFolder().notebook(f.science()).name("mechanics").please();
    snapshotCurrentPortableTree(f.science());

    relationController.moveNoteToFolder(f.force(), mechanics);

    assertLinkingNotebookCommittedOnce(f, "See [[Science:mechanics/Force]].");
  }

  @Test
  void trashRemovingFromPropertiesCommitsTheOwnLinkingNotebookButNotASubscribedOne()
      throws Exception {
    String linkingProperty = "---\ntype: Note\nuses: \"[[Science:Force]]\"\n---\nBody";
    LinkingFixture f = seedForceLinkedFrom(linkingProperty);

    noteController.trashNote(f.force(), removeFromProperties());

    assertLinkingNotebookCommittedOnce(f, "---\ntype: Note\n---\nBody");
    assertThat(contentOf(f.sharedReferrer()), equalTo(linkingProperty));
  }

  @Test
  void folderMoveWithinTheNotebookCommitsTheRewrittenLinkInTheLinkingNotebook() throws Exception {
    LinkingFixture f = seedForceLinkedFrom("physics", "See [[Science:physics/Force]].");
    Folder natural = makeMe.aFolder().notebook(f.science()).name("natural").please();
    snapshotCurrentPortableTree(f.science());

    folderController.moveFolder(f.science(), f.force().getFolder(), folderMove(natural.getId()));

    assertLinkingNotebookCommittedOnce(f, "See [[Science:natural/physics/Force]].");
  }

  @Test
  void folderDissolveCommitsTheRewrittenLinkInTheLinkingNotebook() throws Exception {
    LinkingFixture f = seedForceLinkedFrom("physics", "See [[Science:physics/Force]].");

    folderController.dissolveFolder(f.science(), f.force().getFolder(), false);

    assertLinkingNotebookCommittedOnce(f, "See [[Science:/Force]].");
  }

  @Test
  void moveToAnotherNotebookCommitsTheRewrittenLinkInTheLinkingNotebook() throws Exception {
    LinkingFixture f = seedForceLinkedFrom("See [[Science:Force]].");
    Notebook physics = createGitBackedNotebook("Physics");
    snapshotCurrentPortableTree(physics);

    relationController.moveNoteToNotebookRootInNotebook(f.force(), physics);

    assertLinkingNotebookCommittedOnce(f, "See [[Physics:Force|Science:Force]].");
    assertAcceptedTreeMatchesTheFullAssembly(physics);
  }

  @Test
  void folderMoveToAnotherNotebookCommitsTheRewrittenLinkInTheLinkingNotebook() throws Exception {
    LinkingFixture f = seedForceLinkedFrom("mechanics", "See [[Science:mechanics/Force]].");
    Notebook physics = createGitBackedNotebook("Physics");
    snapshotCurrentPortableTree(physics);

    folderController.moveFolder(f.science(), f.force().getFolder(), folderMoveTo(physics, false));

    assertLinkingNotebookCommittedOnce(f, "See [[Physics:Force|Science:mechanics/Force]].");
    assertAcceptedTreeMatchesTheFullAssembly(physics);
  }

  void assertLinkingNotebookCommittedOnce(LinkingFixture f, String bridgeContent) throws Exception {
    AcceptedHistory engineeringAfter = acceptedHistory(f.engineering());
    assertThat(engineeringAfter.parents(), equalTo(f.engineeringBefore().commits()));
    assertThat(tipText(engineeringAfter, "Bridge.md"), equalTo(bridgeContent));
    assertAcceptedTreeMatchesTheFullAssembly(f.science());
    assertAcceptedTreeMatchesTheFullAssembly(f.engineering());
  }

  LinkingFixture seedForceLinkedFrom(String referrerContent) throws Exception {
    return seedForceLinkedFrom(null, referrerContent);
  }

  LinkingFixture seedForceLinkedFrom(String forceFolder, String referrerContent) throws Exception {
    Notebook science = createGitBackedNotebook("Science");
    Note force =
        forceFolder == null
            ? makeMe.aNote("Force").notebook(science).please()
            : makeMe
                .aNote("Force")
                .folder(makeMe.aFolder().notebook(science).name(forceFolder).please())
                .please();
    Notebook engineering = createGitBackedNotebook("Engineering");
    Note ownReferrer = makeMe.aNote("Bridge").notebook(engineering).please();
    authorReferencingContentCommitted(ownReferrer, referrerContent);
    User other = createFixtureUser();
    Notebook shared = makeMe.aNotebook().name("Shared").creatorAndOwner(other).please();
    Note sharedReferrer = makeMe.aNote("Tower").notebook(shared).please();
    authorReferencingContentCommitted(sharedReferrer, referrerContent);
    User owner = currentUser.getUser();
    owner
        .getSubscriptions()
        .add(makeMe.aSubscription().forNotebook(shared).forUser(owner).please());
    snapshotCurrentPortableTree(science);
    snapshotCurrentPortableTree(engineering);
    return new LinkingFixture(
        science, engineering, acceptedHistory(engineering), force, ownReferrer, sharedReferrer);
  }

  String contentOf(Note note) {
    return noteRepository.findById(note.getId()).orElseThrow().getContent();
  }

  record LinkingFixture(
      Notebook science,
      Notebook engineering,
      AcceptedHistory engineeringBefore,
      Note force,
      Note ownReferrer,
      Note sharedReferrer) {}
}
