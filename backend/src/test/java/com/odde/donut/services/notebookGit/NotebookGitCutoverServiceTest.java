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
import org.eclipse.jgit.revwalk.RevWalk;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

class NotebookGitCutoverServiceTest extends SpringTestBase {
  @Autowired NotebookGitCutoverService notebookGitCutoverService;
  @Autowired NotebookGitBindingRepository notebookGitBindingRepository;
  @Autowired FolderRepository folderRepository;
  @Autowired NotebookGitAcceptedRepositoryStore acceptedRepositoryStore;

  @Test
  void createsOneRootCommitBindingCapturingTheNotebooksCanonicalTree() throws Exception {
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

    Instant cutoverTime = Instant.parse("2026-09-04T10:15:30Z");
    notebookGitCutoverService.createBindingForNotebook(notebook, cutoverTime);
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

      try (RevWalk revWalk = new RevWalk(readBack)) {
        RevCommit commit = revWalk.parseCommit(headObjectId);
        assertThat(commit.getParentCount(), equalTo(0));
        assertThat(commit.getAuthorIdent().getName(), equalTo("Donut System"));
        assertThat(commit.getAuthorIdent().getEmailAddress(), equalTo("system@donut.local"));

        List<PortableTreeEntry> foundEntries = GitBundleTestReader.readExactTree(readBack, commit);

        assertThat(foundEntries, contains(expectedEntries.toArray(new PortableTreeEntry[0])));

        // Only one commit reachable from main: no earlier history was fabricated.
        revWalk.reset();
        revWalk.markStart(commit);
        int commitCount = 0;
        for (RevCommit ignored : revWalk) {
          commitCount++;
        }
        assertThat(commitCount, equalTo(1));
      }
    }
  }

  @Test
  void resetHistoryReplacesTheExistingBindingWithTheNotebooksCurrentContent() throws Exception {
    Notebook notebook = makeMe.aNotebook().please();
    makeMe.entityPersister.flush();
    NotebookGitBinding initialBinding =
        notebookGitCutoverService.createBindingForNotebook(
            notebook, Instant.parse("2026-09-01T00:00:00Z"));
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
    notebookGitCutoverService.resetHistory(notebook, snapshotTime);
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

      try (RevWalk revWalk = new RevWalk(readBack)) {
        RevCommit commit = revWalk.parseCommit(headObjectId);

        List<PortableTreeEntry> foundEntries = GitBundleTestReader.readExactTree(readBack, commit);

        assertThat(foundEntries, contains(expectedEntries.toArray(new PortableTreeEntry[0])));
      }
    }
  }
}
