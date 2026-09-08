package com.odde.donut.controllers;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;

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
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

class NotebookGitNoteCreationFolderControllerTest
    extends NotebookGitNoteCreationControllerTestSupport {

  @Autowired FolderRepository folderRepository;

  @Test
  void unrepresentedEmptyFolderKeepsExistingWebCreationAndAcceptedHead() throws Exception {
    Notebook notebook = createGitBackedNotebook();
    Folder folder = makeMe.aFolder().notebook(notebook).name("Box").please();
    NotebookGitBinding accepted = binding(notebook);
    NoteCreationDTO creation = titleOnly("Nested");
    creation.setFolderId(folder.getId());

    NoteRealm result = controller.createNoteAtNotebookRoot(notebook, creation);

    Note created = noteRepository.findById(result.getId()).orElseThrow();
    assertThat(created.getFolder().getId(), is(folder.getId()));
    Folder preserved = folderRepository.findById(folder.getId()).orElseThrow();
    assertThat(preserved.getName(), is("Box"));
    assertThat(preserved.getReadmeContent(), nullValue());
    assertThat(preserved.getParentFolder(), nullValue());
    assertBindingUnchanged(notebook, accepted);
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
}
