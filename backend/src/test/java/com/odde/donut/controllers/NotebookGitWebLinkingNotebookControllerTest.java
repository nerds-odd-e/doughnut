package com.odde.donut.controllers;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;

import com.odde.donut.controllers.dto.NoteUpdateTitleDTO;
import com.odde.donut.controllers.dto.TitleRenameReferenceHandling;
import com.odde.donut.entities.Note;
import com.odde.donut.entities.Notebook;
import com.odde.donut.entities.User;
import com.odde.donut.exceptions.UnexpectedNoAccessRightException;
import org.junit.jupiter.api.Test;

class NotebookGitWebLinkingNotebookControllerTest extends NotebookGitWebContentControllerTestBase {

  @Test
  void renameRewritesOwnLinkingNotebookButNotASubscribedOne()
      throws UnexpectedNoAccessRightException {
    LinkingFixture f = seedForceLinkedFrom("See [[Science:Force]].");
    NoteUpdateTitleDTO rename = titleDto("Load");
    rename.setReferenceHandling(TitleRenameReferenceHandling.UPDATE_VISIBLE_TEXT);

    textContentController.updateNoteTitle(f.force(), rename);

    assertThat(contentOf(f.ownReferrer()), equalTo("See [[Science:Load]]."));
    assertThat(contentOf(f.sharedReferrer()), equalTo("See [[Science:Force]]."));
  }

  @Test
  void trashRemovingFromPropertiesKeepsTheLinkInASubscribedNotebook()
      throws UnexpectedNoAccessRightException {
    String linkingProperty = "---\ntype: Note\nuses: \"[[Science:Force]]\"\n---\nBody";
    LinkingFixture f = seedForceLinkedFrom(linkingProperty);

    noteController.trashNote(f.force(), removeFromProperties());

    assertThat(contentOf(f.ownReferrer()), equalTo("---\ntype: Note\n---\nBody"));
    assertThat(contentOf(f.sharedReferrer()), equalTo(linkingProperty));
  }

  LinkingFixture seedForceLinkedFrom(String referrerContent)
      throws UnexpectedNoAccessRightException {
    Notebook science = createGitBackedNotebook("Science");
    Note force = makeMe.aNote("Force").notebook(science).please();
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
    return new LinkingFixture(force, ownReferrer, sharedReferrer);
  }

  String contentOf(Note note) {
    return noteRepository.findById(note.getId()).orElseThrow().getContent();
  }

  record LinkingFixture(Note force, Note ownReferrer, Note sharedReferrer) {}
}
