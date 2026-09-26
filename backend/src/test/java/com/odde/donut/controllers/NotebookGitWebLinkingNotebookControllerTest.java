package com.odde.donut.controllers;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;

import com.odde.donut.controllers.dto.NoteUpdateTitleDTO;
import com.odde.donut.controllers.dto.TitleRenameReferenceHandling;
import com.odde.donut.entities.Folder;
import com.odde.donut.entities.Note;
import com.odde.donut.entities.Notebook;
import com.odde.donut.entities.User;
import com.odde.donut.testability.GitBundleTestReader.AcceptedHistory;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

class NotebookGitWebLinkingNotebookControllerTest extends NotebookGitWebContentControllerTestBase {
  @Autowired RelationController relationController;

  @Test
  void renameRewritesOwnLinkingNotebookButNotASubscribedOne() throws Exception {
    LinkingFixture f = seedForceLinkedFrom("See [[Science:Force]].");
    NoteUpdateTitleDTO rename = titleDto("Load");
    rename.setReferenceHandling(TitleRenameReferenceHandling.UPDATE_VISIBLE_TEXT);

    textContentController.updateNoteTitle(f.force(), rename);

    assertThat(contentOf(f.ownReferrer()), equalTo("See [[Science:Load]]."));
    assertThat(contentOf(f.sharedReferrer()), equalTo("See [[Science:Force]]."));
  }

  @Test
  void trashRemovingFromPropertiesKeepsTheLinkInASubscribedNotebook() throws Exception {
    String linkingProperty = "---\ntype: Note\nuses: \"[[Science:Force]]\"\n---\nBody";
    LinkingFixture f = seedForceLinkedFrom(linkingProperty);

    noteController.trashNote(f.force(), removeFromProperties());

    assertThat(contentOf(f.ownReferrer()), equalTo("---\ntype: Note\n---\nBody"));
    assertThat(contentOf(f.sharedReferrer()), equalTo(linkingProperty));
  }

  @Test
  void renameCommitsTheRewrittenLinkInTheLinkingNotebook() throws Exception {
    LinkingFixture f = seedForceLinkedFrom("See [[Science:Force]].");
    NoteUpdateTitleDTO rename = titleDto("Load");
    rename.setReferenceHandling(TitleRenameReferenceHandling.UPDATE_VISIBLE_TEXT);

    textContentController.updateNoteTitle(f.force(), rename);

    assertLinkingNotebookCommittedOnce(f, "See [[Science:Load]].");
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
  void trashRemovingFromPropertiesCommitsTheLinkingNotebookWithoutTheLink() throws Exception {
    LinkingFixture f =
        seedForceLinkedFrom("---\ntype: Note\nuses: \"[[Science:Force]]\"\n---\nBody");

    noteController.trashNote(f.force(), removeFromProperties());

    assertLinkingNotebookCommittedOnce(f, "---\ntype: Note\n---\nBody");
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
