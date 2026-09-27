package com.odde.donut.services.notebookGit;

import static com.odde.donut.services.notebookTree.PortableTreeEntry.ofText;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.not;

import com.odde.donut.entities.Folder;
import com.odde.donut.entities.Notebook;
import com.odde.donut.entities.NotebookGitBinding;
import com.odde.donut.entities.repositories.FolderRepository;
import com.odde.donut.entities.repositories.NotebookGitBindingRepository;
import com.odde.donut.services.notebookTree.PortableTreeEntry;
import com.odde.donut.services.notebookTree.PortableTreeReadmeMarkdown;
import com.odde.donut.testability.GitBundleTestReader;
import com.odde.donut.testability.SpringTestBase;
import java.time.Instant;
import java.util.List;
import org.eclipse.jgit.internal.storage.dfs.DfsRepositoryDescription;
import org.eclipse.jgit.internal.storage.dfs.InMemoryRepository;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.revwalk.RevCommit;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

class NotebookGitHistoryServiceTest extends SpringTestBase {
  @Autowired NotebookGitHistoryService notebookGitHistoryService;
  @Autowired NotebookGitBindingRepository notebookGitBindingRepository;
  @Autowired FolderRepository folderRepository;
  @Autowired NotebookGitAcceptedRepositoryStore acceptedRepositoryStore;

  @Test
  void startHistoryBindsTheNotebooksCanonicalTree() throws Exception {
    Notebook notebook = makeMe.aNotebook().readmeContent("# Notebook readme").please();
    Folder folder =
        makeMe
            .aFolder()
            .notebook(notebook)
            .name("Recipes")
            .readmeContent("# Recipes readme")
            .please();
    makeMe.aNote("Pasta").folder(folder).content("Boil water").please();
    Folder emptyFolder = makeMe.aFolder().notebook(notebook).name("Ideas").please();
    Integer emptyFolderId = emptyFolder.getId();
    makeMe.entityPersister.flush();

    Instant creationTime = Instant.parse("2026-09-04T10:15:30Z");
    notebookGitHistoryService.startHistory(notebook, creationTime);
    makeMe.entityPersister.flushAndClear();

    NotebookGitBinding binding =
        notebookGitBindingRepository.findByNotebook_Id(notebook.getId()).orElseThrow();

    List<PortableTreeEntry> expectedEntries =
        List.of(
            NotebookGitAttributes.initialEntry(),
            ofText("Ideas/.keep", ""),
            ofText("README.md", PortableTreeReadmeMarkdown.assemble("# Notebook readme")),
            ofText("Recipes/Pasta.md", "Boil water"),
            ofText("Recipes/README.md", PortableTreeReadmeMarkdown.assemble("# Recipes readme")));

    assertThat(
        folderRepository.findById(emptyFolderId).orElseThrow().getId(), equalTo(emptyFolderId));

    try (InMemoryRepository readBack = new InMemoryRepository(new DfsRepositoryDescription())) {
      ObjectId headObjectId =
          GitBundleTestReader.fetchHead(
              readBack, acceptedRepositoryStore.downloadableBundle(binding));
      assertThat(headObjectId.getName(), equalTo(binding.getAcceptedGitObjectId()));

      RevCommit commit = readBack.parseCommit(headObjectId);
      assertThat(
          GitBundleTestReader.readExactTree(readBack, commit),
          contains(expectedEntries.toArray(new PortableTreeEntry[0])));
    }
  }

  @Test
  void resetHistoryReplacesTheExistingBindingWithTheNotebooksCurrentContent() throws Exception {
    Notebook notebook = makeMe.aNotebook().please();
    makeMe.entityPersister.flush();
    NotebookGitBinding initialBinding =
        notebookGitHistoryService.startHistory(notebook, Instant.parse("2026-09-01T00:00:00Z"));
    Integer initialBindingId = initialBinding.getId();
    String initialGitObjectId = initialBinding.getAcceptedGitObjectId();

    notebook.setReadmeContent("# Notebook readme");
    Folder folder =
        makeMe
            .aFolder()
            .notebook(notebook)
            .name("Recipes")
            .readmeContent("# Recipes readme")
            .please();
    makeMe.aNote("Pasta").folder(folder).content("Boil water").please();
    makeMe.entityPersister.flush();

    Instant snapshotTime = Instant.parse("2026-09-04T10:15:30Z");
    notebookGitHistoryService.resetHistory(notebook, snapshotTime);
    makeMe.entityPersister.flushAndClear();

    NotebookGitBinding binding =
        notebookGitBindingRepository.findByNotebook_Id(notebook.getId()).orElseThrow();
    assertThat(binding.getId(), equalTo(initialBindingId));
    assertThat(binding.getAcceptedGitObjectId(), not(equalTo(initialGitObjectId)));

    List<PortableTreeEntry> expectedEntries =
        List.of(
            NotebookGitAttributes.initialEntry(),
            ofText("README.md", PortableTreeReadmeMarkdown.assemble("# Notebook readme")),
            ofText("Recipes/Pasta.md", "Boil water"),
            ofText("Recipes/README.md", PortableTreeReadmeMarkdown.assemble("# Recipes readme")));

    try (InMemoryRepository readBack = new InMemoryRepository(new DfsRepositoryDescription())) {
      ObjectId headObjectId =
          GitBundleTestReader.fetchHead(
              readBack, acceptedRepositoryStore.downloadableBundle(binding));
      assertThat(headObjectId.getName(), equalTo(binding.getAcceptedGitObjectId()));

      RevCommit commit = readBack.parseCommit(headObjectId);
      assertThat(
          GitBundleTestReader.readExactTree(readBack, commit),
          contains(expectedEntries.toArray(new PortableTreeEntry[0])));
    }
  }
}
