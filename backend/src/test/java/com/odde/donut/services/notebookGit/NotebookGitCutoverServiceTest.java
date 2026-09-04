package com.odde.donut.services.notebookGit;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.equalTo;

import com.odde.donut.entities.Folder;
import com.odde.donut.entities.Notebook;
import com.odde.donut.entities.NotebookGitBinding;
import com.odde.donut.entities.repositories.NotebookGitBindingRepository;
import com.odde.donut.services.notebookExport.ExportFolderRow;
import com.odde.donut.services.notebookExport.ExportNoteRow;
import com.odde.donut.services.notebookExport.PortableTreeEntry;
import com.odde.donut.services.notebookExport.PortableTreeSnapshot;
import com.odde.donut.testability.MakeMe;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import org.eclipse.jgit.internal.storage.dfs.DfsRepositoryDescription;
import org.eclipse.jgit.internal.storage.dfs.InMemoryRepository;
import org.eclipse.jgit.lib.NullProgressMonitor;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.ObjectLoader;
import org.eclipse.jgit.lib.Ref;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.revwalk.RevWalk;
import org.eclipse.jgit.transport.FetchConnection;
import org.eclipse.jgit.transport.TransportBundleStream;
import org.eclipse.jgit.transport.URIish;
import org.eclipse.jgit.treewalk.TreeWalk;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class NotebookGitCutoverServiceTest {

  @Autowired MakeMe makeMe;
  @Autowired NotebookGitCutoverService notebookGitCutoverService;
  @Autowired NotebookGitBindingRepository notebookGitBindingRepository;

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
    makeMe.entityPersister.flush();

    Instant cutoverTime = Instant.parse("2026-09-04T10:15:30Z");
    notebookGitCutoverService.createBindingForNotebook(notebook, cutoverTime);
    makeMe.entityPersister.flushAndClear();

    NotebookGitBinding binding =
        notebookGitBindingRepository.findByNotebook_Id(notebook.getId()).orElseThrow();

    List<PortableTreeEntry> expectedEntries =
        PortableTreeSnapshot.build(
            "# Notebook readme",
            List.of(new ExportFolderRow(folder.getId(), null, "Recipes", "# Recipes readme")),
            List.of(new ExportNoteRow(folder.getId(), "Pasta", "Boil water")));

    try (InMemoryRepository readBack = new InMemoryRepository(new DfsRepositoryDescription())) {
      ObjectId headObjectId = fetchBundleInto(readBack, binding.getBundleBytes());
      assertThat(headObjectId.getName(), equalTo(binding.getAcceptedGitObjectId()));

      try (RevWalk revWalk = new RevWalk(readBack)) {
        RevCommit commit = revWalk.parseCommit(headObjectId);
        assertThat(commit.getParentCount(), equalTo(0));
        assertThat(commit.getAuthorIdent().getName(), equalTo("Donut System"));
        assertThat(commit.getAuthorIdent().getEmailAddress(), equalTo("system@donut.local"));

        List<PortableTreeEntry> foundEntries = new ArrayList<>();
        try (TreeWalk treeWalk = new TreeWalk(readBack)) {
          treeWalk.addTree(commit.getTree());
          treeWalk.setRecursive(true);
          while (treeWalk.next()) {
            ObjectId blobId = treeWalk.getObjectId(0);
            ObjectLoader loader = readBack.open(blobId);
            String content = new String(loader.getBytes(), StandardCharsets.UTF_8);
            foundEntries.add(new PortableTreeEntry(treeWalk.getPathString(), content));
          }
        }

        List<PortableTreeEntry> sortedExpected =
            expectedEntries.stream().sorted((a, b) -> a.path().compareTo(b.path())).toList();
        assertThat(foundEntries, contains(sortedExpected.toArray(new PortableTreeEntry[0])));

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

  private static ObjectId fetchBundleInto(InMemoryRepository target, byte[] bundleBytes)
      throws IOException, URISyntaxException {
    try (TransportBundleStream transport =
            new TransportBundleStream(
                target, new URIish("in-memory:bundle"), new ByteArrayInputStream(bundleBytes));
        FetchConnection fetchConnection = transport.openFetch()) {
      Ref mainRef = fetchConnection.getRef("refs/heads/main");
      fetchConnection.fetch(NullProgressMonitor.INSTANCE, List.of(mainRef), Set.of());
      return mainRef.getObjectId();
    }
  }
}
