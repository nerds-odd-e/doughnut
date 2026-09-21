package com.odde.donut.controllers;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.nullValue;

import com.odde.donut.entities.Folder;
import com.odde.donut.entities.Note;
import com.odde.donut.entities.Notebook;
import com.odde.donut.entities.NotebookGitBinding;
import com.odde.donut.entities.repositories.FolderRepository;
import com.odde.donut.services.notebookExport.ExportReadmeMarkdown;
import com.odde.donut.testability.GitBundleTestReader;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.eclipse.jgit.internal.storage.dfs.DfsRepositoryDescription;
import org.eclipse.jgit.internal.storage.dfs.InMemoryRepository;
import org.eclipse.jgit.lib.ObjectId;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Verifies one publication of an exact folder relocation followed by a descendant edit and note
 * addition retains Folder/Note identities at the final hierarchy, including when the destination
 * parent was represented earlier in the range.
 */
class NotebookGitComposedFolderRelocationControllerTest extends NotebookGitControllerTestBase {

  private static final String README_BODY = "readme";
  private static final String README = ExportReadmeMarkdown.assemble(README_BODY);
  private static final String NOTE = "---\ntype: Note\n---\nnote";
  private static final String EDITED = "---\ntype: Note\n---\nedited";
  private static final String ADDED = "---\ntype: Note\n---\nadded";
  private static final String ARCHIVE_README = ExportReadmeMarkdown.assemble("Archive landing");

  @Autowired FolderRepository folderRepository;

  @Test
  void publishesRelocateThenDescendantEditAndAddRetainingIdentitiesWithEarlierParent()
      throws Exception {
    Notebook notebook = createGitBackedNotebook();
    Folder topics =
        makeMe.aFolder().notebook(notebook).name("Topics").readmeContent(README_BODY).please();
    Folder sub = makeMe.aFolder().parentFolder(topics).name("Sub").please();
    Note nested = makeMe.aNote().folder(topics).title("A").content(NOTE).please();
    Note deeper = makeMe.aNote().folder(sub).title("B").content(NOTE).please();
    NotebookGitBinding binding = snapshotCurrentPortableTree(notebook);
    ObjectId acceptedHead = ObjectId.fromString(binding.getAcceptedGitObjectId());
    ObjectId tip;
    byte[] proposalBytes;
    try (InMemoryRepository repository = new InMemoryRepository(new DfsRepositoryDescription())) {
      GitBundleTestReader.fetchHead(repository, acceptedBundleBytes(notebook));
      ObjectId afterParent =
          commitOnTopOf(
              repository,
              List.of(acceptedHead),
              List.of(
                  new NotebookGitProposalFile("Topics/README.md", README),
                  new NotebookGitProposalFile("Topics/A.md", NOTE),
                  new NotebookGitProposalFile("Topics/Sub/B.md", NOTE),
                  new NotebookGitProposalFile("Archive/README.md", ARCHIVE_README)),
              "Add Archive parent");
      ObjectId afterRelocate =
          commitOnTopOf(
              repository,
              List.of(afterParent),
              List.of(
                  new NotebookGitProposalFile("Archive/README.md", ARCHIVE_README),
                  new NotebookGitProposalFile("Archive/Topics/README.md", README),
                  new NotebookGitProposalFile("Archive/Topics/A.md", NOTE),
                  new NotebookGitProposalFile("Archive/Topics/Sub/B.md", NOTE)),
              "Relocate Topics under Archive");
      tip =
          commitOnTopOf(
              repository,
              List.of(afterRelocate),
              List.of(
                  new NotebookGitProposalFile("Archive/README.md", ARCHIVE_README),
                  new NotebookGitProposalFile("Archive/Topics/README.md", README),
                  new NotebookGitProposalFile("Archive/Topics/A.md", EDITED),
                  new NotebookGitProposalFile("Archive/Topics/Sub/B.md", NOTE),
                  new NotebookGitProposalFile("Archive/Topics/Extra.md", ADDED)),
              "Edit descendant and add note");
      proposalBytes = bundleBytesForHead(repository, tip);
    }

    String publishedHead =
        controller.publishNotebookGitProposal(
            notebook.getId(), binding.getAcceptedGitObjectId(), proposalBytes);

    assertThat(publishedHead, equalTo(tip.getName()));
    Map<Integer, Folder> folders = foldersById(notebook);
    Folder archive =
        folders.values().stream()
            .filter(folder -> "Archive".equals(folder.getName()))
            .findFirst()
            .orElseThrow();
    assertThat(folders.get(topics.getId()).getParentFolderId(), equalTo(archive.getId()));
    assertThat(folders.get(sub.getId()).getParentFolderId(), equalTo(topics.getId()));
    assertThat(archive.getParentFolderId(), nullValue());
    assertThat(folders.keySet(), containsInAnyOrder(archive.getId(), topics.getId(), sub.getId()));

    List<Note> notes = noteRepository.findAllByNotebookIdOrderByIdAsc(notebook.getId());
    assertThat(
        notes.stream().map(Note::getId).toList(),
        containsInAnyOrder(
            nested.getId(),
            deeper.getId(),
            notes.stream()
                .filter(note -> "Extra".equals(note.getTitle()))
                .findFirst()
                .orElseThrow()
                .getId()));
    assertThat(noteRepository.findById(nested.getId()).orElseThrow().getContent(), equalTo(EDITED));
    assertThat(noteRepository.findById(deeper.getId()).orElseThrow().getContent(), equalTo(NOTE));
    Note extra =
        notes.stream().filter(note -> "Extra".equals(note.getTitle())).findFirst().orElseThrow();
    assertThat(extra.getContent(), equalTo(ADDED));
    assertThat(extra.getFolder().getId(), equalTo(topics.getId()));
  }

  private Map<Integer, Folder> foldersById(Notebook notebook) {
    return folderRepository.findByNotebookIdOrderByIdAsc(notebook.getId()).stream()
        .collect(Collectors.toMap(Folder::getId, Function.identity()));
  }
}
