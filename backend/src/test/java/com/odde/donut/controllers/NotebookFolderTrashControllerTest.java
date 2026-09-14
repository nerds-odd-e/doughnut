package com.odde.donut.controllers;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.iterableWithSize;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.odde.donut.controllers.dto.SearchTerm;
import com.odde.donut.entities.Folder;
import com.odde.donut.entities.MemoryTracker;
import com.odde.donut.entities.Note;
import com.odde.donut.entities.Notebook;
import com.odde.donut.entities.repositories.FolderRepository;
import com.odde.donut.exceptions.UnexpectedNoAccessRightException;
import com.odde.donut.testability.RelationshipLiteralSearchHits;
import java.sql.Timestamp;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

class NotebookFolderTrashControllerTest extends NotebookControllerTestBase {

  @Autowired FolderRepository folderRepository;
  @Autowired NoteController noteController;
  @Autowired SearchController searchController;
  @Autowired RecallsController recallsController;

  @Test
  void trashesTheRetainedSubtreeBelowItsMirroredAncestorPath()
      throws UnexpectedNoAccessRightException {
    Notebook notebook = ownedNotebook();
    Folder research = ownedFolder(notebook, "Research");
    Folder biology = makeMe.aFolder().parentFolder(research).name("Biology").please();
    Folder topic =
        makeMe.aFolder().parentFolder(biology).name("Topic").readmeContent("# retained").please();
    Folder empty = makeMe.aFolder().parentFolder(topic).name("Empty").please();
    Note note = makeMe.aNote("Cells").folder(topic).content("authored [[Cells]]").please();
    Note referrer = makeMe.aNote("Referrer").notebook(notebook).please();
    authorReferencingContent(referrer, "See [[Research/Biology/Topic/Cells]].");
    Timestamp now = makeMe.aTimestamp().of(1, 8).please();
    testabilitySettings.timeTravelTo(now);
    MemoryTracker tracker =
        makeMe.aMemoryTrackerFor(note).assimilatedAt(now).recallCount(1).please();
    Integer topicId = topic.getId();
    Integer noteId = note.getId();
    String referrerContent = referrer.getContent();

    Folder result = controller.trashFolder(notebook, topic);

    makeMe.refresh(topic);
    makeMe.refresh(empty);
    makeMe.refresh(note);
    assertThat(result.getId(), equalTo(topicId));
    assertThat(topic.getParentFolder().getName(), equalTo("Biology"));
    assertThat(topic.getParentFolder().getParentFolder().getName(), equalTo("Research"));
    assertThat(
        topic.getParentFolder().getParentFolder().getParentFolder().getName(), equalTo("_trash"));
    assertThat(topic.getReadmeContent(), equalTo("# retained"));
    assertThat(empty.getParentFolder().getId(), equalTo(topicId));
    assertThat(note.getId(), equalTo(noteId));
    assertThat(note.getContent(), equalTo("authored [[Cells]]"));
    assertThat(referrer.getContent(), equalTo(referrerContent));
    assertThat(note.isAvailable(), equalTo(false));
    assertThat(
        RelationshipLiteralSearchHits.noteMatches(
            searchController.searchForRelationshipTarget(searchTerm("Cells"))),
        hasSize(0));
    assertThat(noteController.showNote(referrer).getWikiLinks(), hasSize(0));
    assertThat(
        recallsController.recalling("Asia/Shanghai", 0).getToRepeat().stream()
            .noneMatch(candidate -> tracker.getId().equals(candidate.getMemoryTrackerId())),
        equalTo(true));
  }

  @Test
  void mismatchedNotebookCreatesNoTrashFolders() {
    Notebook requestedNotebook = ownedNotebook();
    Folder foreignFolder = ownedFolder(ownedNotebook(), "Foreign");
    int originalCount = folderCount();

    ResponseStatusException error =
        assertThrows(
            ResponseStatusException.class,
            () -> controller.trashFolder(requestedNotebook, foreignFolder));

    assertThat(error.getStatusCode(), equalTo(HttpStatus.NOT_FOUND));
    assertThat(folderRepository.findAll(), iterableWithSize(originalCount));
    assertThat(foreignFolder.getParentFolder(), equalTo(null));
  }

  @Test
  void refusesAlreadyTrashedFolderWithoutChangingEarlierTrash() {
    Notebook notebook = ownedNotebook();
    Folder trash = ownedFolder(notebook, "_trash");
    Folder trashed = makeMe.aFolder().parentFolder(trash).name("Earlier").please();
    int originalCount = folderCount();

    ResponseStatusException error =
        assertThrows(
            ResponseStatusException.class, () -> controller.trashFolder(notebook, trashed));

    assertThat(error.getStatusCode(), equalTo(HttpStatus.BAD_REQUEST));
    assertThat(folderRepository.findAll(), iterableWithSize(originalCount));
    assertThat(trashed.getParentFolder().getId(), equalTo(trash.getId()));
  }

  @Test
  void usesFirstFreeSiblingNameWithExistingCaseRulesWithoutChangingEarlierTrash()
      throws UnexpectedNoAccessRightException {
    Notebook notebook = ownedNotebook();
    Folder trash = ownedFolder(notebook, "_trash");
    Folder earlier = makeMe.aFolder().parentFolder(trash).name("Biology").please();
    Note earlierNote = makeMe.aNote("Earlier").folder(earlier).please();
    makeMe.aFolder().parentFolder(trash).name("biology (2)").please();
    Folder later = makeMe.aFolder().parentFolder(trash).name("Biology (3)").please();
    Note laterNote = makeMe.aNote("Later").folder(later).please();
    Folder incoming = ownedFolder(notebook, "Biology");
    Note incomingNote = makeMe.aNote("Incoming").folder(incoming).please();
    Integer incomingId = incoming.getId();

    Folder result = controller.trashFolder(notebook, incoming);

    makeMe.refresh(earlierNote);
    makeMe.refresh(laterNote);
    makeMe.refresh(incomingNote);
    assertThat(result.getId(), equalTo(incomingId));
    assertThat(result.getName(), equalTo("Biology (2)"));
    assertThat(result.getParentFolder().getId(), equalTo(trash.getId()));
    assertThat(incomingNote.getFolder().getId(), equalTo(incoming.getId()));
    assertThat(earlierNote.getFolder().getId(), equalTo(earlier.getId()));
    assertThat(laterNote.getFolder().getId(), equalTo(later.getId()));
  }

  @Test
  void suffixFitsTheFolderNameLengthLimit() throws UnexpectedNoAccessRightException {
    Notebook notebook = ownedNotebook();
    String longestName = makeMe.aStringOfLength(Folder.MAX_NAME_LENGTH);
    Folder incoming = ownedFolder(notebook, longestName);
    Folder trash = ownedFolder(notebook, "_trash");
    makeMe.aFolder().parentFolder(trash).name(longestName).please();

    Folder result = controller.trashFolder(notebook, incoming);

    assertThat(
        result.getName(),
        equalTo(longestName.substring(0, Folder.MAX_NAME_LENGTH - " (2)".length()) + " (2)"));
  }

  private int folderCount() {
    return Math.toIntExact(folderRepository.count());
  }

  private SearchTerm searchTerm(String searchKey) {
    SearchTerm searchTerm = new SearchTerm();
    searchTerm.setSearchKey(searchKey);
    searchTerm.setAllMyNotebooksAndSubscriptions(true);
    return searchTerm;
  }
}
