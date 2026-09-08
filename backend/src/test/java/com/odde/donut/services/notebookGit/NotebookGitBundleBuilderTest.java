package com.odde.donut.services.notebookGit;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.notNullValue;

import com.odde.donut.services.notebookExport.PortableTreeEntry;
import com.odde.donut.services.notebookGit.NotebookGitBundleWriter.BundleWriteResult;
import com.odde.donut.testability.GitBundleTestReader;
import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import org.eclipse.jgit.internal.storage.dfs.DfsRepositoryDescription;
import org.eclipse.jgit.internal.storage.dfs.InMemoryRepository;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.ObjectLoader;
import org.eclipse.jgit.lib.Ref;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.revwalk.RevWalk;
import org.eclipse.jgit.treewalk.TreeWalk;
import org.junit.jupiter.api.Test;

class NotebookGitBundleBuilderTest {

  @Test
  void buildsSingleRootCommitOnMainWithMatchingPathsAndContent() throws IOException {
    List<PortableTreeEntry> entries =
        List.of(
            new PortableTreeEntry("Parent Folder/README.md", "Parent readme"),
            new PortableTreeEntry("Parent Folder/Child Folder/Nested note.md", "Nested body"));

    Instant commitTime = Instant.parse("2026-09-04T10:15:30Z");

    try (Repository repository =
        NotebookGitBundleBuilder.build(
            entries, "Donut System", "system@donut.local", "Snapshot import", commitTime)) {
      Ref mainRef = repository.exactRef("refs/heads/main");
      assertThat(mainRef, notNullValue());

      try (RevWalk revWalk = new RevWalk(repository)) {
        RevCommit commit = revWalk.parseCommit(mainRef.getObjectId());
        assertThat(commit.getParentCount(), equalTo(0));
        assertThat(commit.getAuthorIdent().getName(), equalTo("Donut System"));
        assertThat(commit.getAuthorIdent().getEmailAddress(), equalTo("system@donut.local"));
        assertThat(commit.getFullMessage(), equalTo("Snapshot import"));

        List<PortableTreeEntry> found = new ArrayList<>();
        try (TreeWalk treeWalk = new TreeWalk(repository)) {
          treeWalk.addTree(commit.getTree());
          treeWalk.setRecursive(true);
          while (treeWalk.next()) {
            ObjectId blobId = treeWalk.getObjectId(0);
            ObjectLoader loader = repository.open(blobId);
            String content = new String(loader.getBytes(), StandardCharsets.UTF_8);
            found.add(new PortableTreeEntry(treeWalk.getPathString(), content));
          }
        }

        assertThat(
            found,
            contains(
                new PortableTreeEntry("Parent Folder/Child Folder/Nested note.md", "Nested body"),
                new PortableTreeEntry("Parent Folder/README.md", "Parent readme")));
      }
    }
  }

  @Test
  void replacesTipKeepingParentAndAuthorThenRoundTripsThroughBundle()
      throws IOException, URISyntaxException {
    Instant ancestorTime = Instant.parse("2026-09-04T10:00:00Z");
    Instant tipAuthorTime = Instant.parse("2026-09-04T10:05:00Z");
    Instant replaceCommitterTime = Instant.parse("2026-09-04T10:08:00Z");

    try (Repository repository =
        NotebookGitBundleBuilder.build(
            List.of(new PortableTreeEntry("Note.md", "v0")),
            "Donut System",
            "system@donut.local",
            "Cutover",
            ancestorTime)) {
      ObjectId ancestor = repository.exactRef("refs/heads/main").getObjectId();
      ObjectId tip =
          NotebookGitBundleBuilder.append(
              repository,
              ancestor,
              List.of(new PortableTreeEntry("Note.md", "v1")),
              "Donut System",
              "system@donut.local",
              "Edit note",
              tipAuthorTime);

      ObjectId replacement =
          NotebookGitBundleBuilder.replaceTip(
              repository,
              tip,
              List.of(new PortableTreeEntry("Note.md", "v2")),
              "Edit note",
              replaceCommitterTime);

      BundleWriteResult written = NotebookGitBundleWriter.write(repository);
      assertThat(written.headObjectId(), equalTo(replacement.getName()));

      try (InMemoryRepository readBack = new InMemoryRepository(new DfsRepositoryDescription())) {
        ObjectId selectedTip = GitBundleTestReader.fetchHead(readBack, written.bundleBytes());
        assertThat(selectedTip, equalTo(replacement));

        try (RevWalk revWalk = new RevWalk(readBack)) {
          RevCommit commit = revWalk.parseCommit(selectedTip);
          assertThat(commit.getParentCount(), equalTo(1));
          assertThat(commit.getParent(0).getId(), equalTo(ancestor));
          assertThat(commit.getAuthorIdent().getName(), equalTo("Donut System"));
          assertThat(commit.getAuthorIdent().getEmailAddress(), equalTo("system@donut.local"));
          assertThat(commit.getAuthorIdent().getWhenAsInstant(), equalTo(tipAuthorTime));
          assertThat(commit.getCommitterIdent().getWhenAsInstant(), equalTo(replaceCommitterTime));
          assertThat(commit.getFullMessage(), equalTo("Edit note"));
          assertThat(
              new String(
                  readBack
                      .open(GitBundleTestReader.blobIdAt(readBack, selectedTip, "Note.md"))
                      .getBytes(),
                  StandardCharsets.UTF_8),
              equalTo("v2"));
        }
      }
    }
  }
}
