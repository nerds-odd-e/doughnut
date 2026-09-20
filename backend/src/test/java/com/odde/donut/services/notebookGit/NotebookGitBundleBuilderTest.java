package com.odde.donut.services.notebookGit;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.notNullValue;

import com.odde.donut.services.notebookExport.PortableTreeEntry;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
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
  void appendsCompleteSnapshotWithTheSameTreeAsAFreshBuild() throws IOException {
    List<PortableTreeEntry> accepted =
        List.of(
            new PortableTreeEntry("保持.md", "Unicode 🌙\n"),
            new PortableTreeEntry("Edit.md", "Before"),
            new PortableTreeEntry("Delete.md", "Gone"),
            new PortableTreeEntry("Empty/.keep", ""),
            new PortableTreeEntry("Emptied/Note.md", "Gone"));
    List<PortableTreeEntry> entries =
        List.of(
            new PortableTreeEntry("保持.md", "Unicode 🌙\n"),
            new PortableTreeEntry("Edit.md", "After"),
            new PortableTreeEntry("Add.md", "New"),
            new PortableTreeEntry("Empty/Note.md", "Occupied"),
            new PortableTreeEntry("Emptied/.keep", ""));
    Instant time = Instant.parse("2026-09-04T10:15:30Z");
    try (Repository repository =
            NotebookGitBundleBuilder.build(
                accepted, "Donut", "system@donut.local", "Initial", time);
        Repository fresh =
            NotebookGitBundleBuilder.build(entries, "Donut", "system@donut.local", "Fresh", time);
        RevWalk walk = new RevWalk(repository);
        RevWalk freshWalk = new RevWalk(fresh)) {
      ObjectId parent = repository.resolve("refs/heads/main");
      ObjectId appended =
          NotebookGitBundleBuilder.append(
              repository,
              parent,
              accepted,
              entries,
              "Donut",
              "system@donut.local",
              "Edit",
              time.plusSeconds(1));
      RevCommit commit = walk.parseCommit(appended);
      assertThat(
          commit.getTree().getId(),
          equalTo(freshWalk.parseCommit(fresh.resolve("refs/heads/main")).getTree().getId()));
      assertThat(
          List.of(commit.getParents()).stream().map(RevCommit::getId).toList(), contains(parent));
    }
  }
}
