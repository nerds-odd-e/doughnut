package com.odde.donut.controllers;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.odde.donut.controllers.dto.FolderCreationRequest;
import com.odde.donut.controllers.dto.NoteCreationDTO;
import com.odde.donut.controllers.dto.NoteRealm;
import com.odde.donut.entities.Folder;
import com.odde.donut.entities.Note;
import com.odde.donut.entities.Notebook;
import com.odde.donut.entities.NotebookGitBinding;
import com.odde.donut.entities.repositories.FolderRepository;
import com.odde.donut.testability.GitBundleTestReader;
import org.eclipse.jgit.internal.storage.dfs.DfsRepositoryDescription;
import org.eclipse.jgit.internal.storage.dfs.InMemoryRepository;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.revwalk.RevWalk;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.server.ResponseStatusException;

class NotebookGitNoteCreationFolderControllerTest
    extends NotebookGitNoteCreationControllerTestSupport {

  @Autowired FolderRepository folderRepository;

  @Test
  void firstNoteInWebCreatedEmptyFolderReplacesMarkerAndRetainsHistory() throws Exception {
    Notebook notebook = createGitBackedNotebook();
    ObjectId originalHead = ObjectId.fromString(binding(notebook).getAcceptedGitObjectId());
    FolderCreationRequest folderCreation = new FolderCreationRequest();
    folderCreation.setName("Biology");
    Folder folder = folderController.createFolder(notebook, folderCreation);
    ObjectId folderHead = ObjectId.fromString(binding(notebook).getAcceptedGitObjectId());
    NoteCreationDTO creation = titleOnly("Cells");
    creation.setFolderId(folder.getId());

    NoteRealm result = controller.createNoteAtNotebookRoot(notebook, creation);

    Note created = noteRepository.findById(result.getId()).orElseThrow();
    assertThat(created.getFolder().getId(), is(folder.getId()));

    byte[] downloaded = controller.downloadNotebookGitBundle(notebook).getBody();
    try (InMemoryRepository repository = new InMemoryRepository(new DfsRepositoryDescription())) {
      ObjectId noteHead = GitBundleTestReader.fetchHead(repository, downloaded);
      assertThat(GitBundleTestReader.pathsIn(repository, noteHead), contains("Biology/Cells.md"));
      try (RevWalk revWalk = new RevWalk(repository)) {
        RevCommit noteCommit = revWalk.parseCommit(noteHead);
        assertThat(noteCommit.getParentCount(), is(1));
        assertThat(noteCommit.getParent(0).getId(), equalTo(folderHead));
        RevCommit folderCommit = revWalk.parseCommit(folderHead);
        assertThat(folderCommit.getParentCount(), is(1));
        assertThat(folderCommit.getParent(0).getId(), equalTo(originalHead));
      }
    }
  }

  @Test
  void representedNestedFolderWithoutReadmeAcceptsAtFullPortablePath() throws Exception {
    Notebook notebook = createGitBackedNotebook();
    Folder fieldNotes = makeMe.aFolder().notebook(notebook).name("Field Notes").please();
    Folder alpine = makeMe.aFolder().parentFolder(fieldNotes).name("Alpine").please();
    makeMe.aNote().folder(alpine).title("Observation").please();
    NotebookGitBinding accepted = snapshotCurrentPortableTree(notebook);
    ObjectId acceptedHead = ObjectId.fromString(accepted.getAcceptedGitObjectId());
    NoteCreationDTO creation = titleOnly("Seedling");
    creation.setFolderId(alpine.getId());

    NoteRealm result = controller.createNoteAtNotebookRoot(notebook, creation);

    Note created = noteRepository.findById(result.getId()).orElseThrow();
    assertThat(created.getFolder().getId(), is(alpine.getId()));
    Folder preservedAlpine = folderRepository.findById(alpine.getId()).orElseThrow();
    Folder preservedFieldNotes = folderRepository.findById(fieldNotes.getId()).orElseThrow();
    assertThat(preservedAlpine.getParentFolder().getId(), is(fieldNotes.getId()));
    assertThat(preservedAlpine.getReadmeContent(), nullValue());
    assertThat(preservedFieldNotes.getReadmeContent(), nullValue());

    byte[] downloaded =
        controller
            .downloadNotebookGitBundle(notebookRepository.findById(notebook.getId()).orElseThrow())
            .getBody();
    try (InMemoryRepository repository = new InMemoryRepository(new DfsRepositoryDescription())) {
      ObjectId newHead = GitBundleTestReader.fetchHead(repository, downloaded);
      assertThat(
          GitBundleTestReader.pathsIn(repository, newHead),
          containsInAnyOrder(
              "Field Notes/Alpine/Observation.md", "Field Notes/Alpine/Seedling.md"));
      assertThat(
          GitBundleTestReader.blobIdAt(repository, newHead, "Field Notes/Alpine/Observation.md"),
          is(
              GitBundleTestReader.blobIdAt(
                  repository, acceptedHead, "Field Notes/Alpine/Observation.md")));
    }
  }

  @Test
  void readmeOnlyFolderAcceptsWithoutChangingReadmeBytes() throws Exception {
    Notebook notebook = createGitBackedNotebook();
    Folder box =
        makeMe.aFolder().notebook(notebook).name("Box").readmeContent("Box notes").please();
    NotebookGitBinding accepted = snapshotCurrentPortableTree(notebook);
    ObjectId acceptedHead = ObjectId.fromString(accepted.getAcceptedGitObjectId());
    NoteCreationDTO creation = titleOnly("Nested");
    creation.setFolderId(box.getId());

    NoteRealm result = controller.createNoteAtNotebookRoot(notebook, creation);

    Note created = noteRepository.findById(result.getId()).orElseThrow();
    assertThat(created.getFolder().getId(), is(box.getId()));
    Folder preserved = folderRepository.findById(box.getId()).orElseThrow();
    assertThat(preserved.getReadmeContent(), is("Box notes"));

    byte[] downloaded =
        controller
            .downloadNotebookGitBundle(notebookRepository.findById(notebook.getId()).orElseThrow())
            .getBody();
    try (InMemoryRepository repository = new InMemoryRepository(new DfsRepositoryDescription())) {
      ObjectId newHead = GitBundleTestReader.fetchHead(repository, downloaded);
      assertThat(
          GitBundleTestReader.pathsIn(repository, newHead),
          containsInAnyOrder("Box/README.md", "Box/Nested.md"));
      assertThat(
          GitBundleTestReader.blobIdAt(repository, newHead, "Box/README.md"),
          is(GitBundleTestReader.blobIdAt(repository, acceptedHead, "Box/README.md")));
    }
  }

  @Test
  void driftedDestinationFolderKeepsWebCreationAndAcceptedHeadUnchanged() throws Exception {
    Notebook notebook = createGitBackedNotebook();
    NotebookGitBinding accepted = binding(notebook);
    Folder unsynchronized = makeMe.aFolder().notebook(notebook).name("Unsynchronized").please();
    NoteCreationDTO creation = titleOnly("Inside Drifted Folder");
    creation.setFolderId(unsynchronized.getId());

    NoteRealm result = controller.createNoteAtNotebookRoot(notebook, creation);

    Note created = noteRepository.findById(result.getId()).orElseThrow();
    assertThat(created.getFolder().getId(), is(unsynchronized.getId()));
    assertBindingUnchanged(notebook, accepted);
  }

  @Test
  void absentDestinationFolderIsRefusedBeforeAnyAcceptedChange() throws Exception {
    Notebook notebook = createGitBackedNotebook();
    NotebookGitBinding accepted = binding(notebook);
    NoteCreationDTO creation = titleOnly("Nowhere");
    creation.setFolderId(-1);

    assertThrows(
        ResponseStatusException.class,
        () -> controller.createNoteAtNotebookRoot(notebook, creation));
    assertBindingUnchanged(notebook, accepted);
  }

  @Test
  void foreignNotebookDestinationFolderIsRefusedBeforeAnyAcceptedChange() throws Exception {
    Notebook notebook = createGitBackedNotebook();
    Notebook otherNotebook = createGitBackedNotebook("Other Git Backed Notebook");
    Folder foreignFolder = makeMe.aFolder().notebook(otherNotebook).name("Foreign").please();
    NotebookGitBinding accepted = binding(notebook);
    NoteCreationDTO creation = titleOnly("Wrong Notebook");
    creation.setFolderId(foreignFolder.getId());

    assertThrows(
        ResponseStatusException.class,
        () -> controller.createNoteAtNotebookRoot(notebook, creation));
    assertBindingUnchanged(notebook, accepted);
  }
}
