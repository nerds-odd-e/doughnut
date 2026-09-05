package com.odde.donut.services.notebookGit;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.ObjectLoader;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.revwalk.RevWalk;
import org.eclipse.jgit.treewalk.TreeWalk;

/**
 * Reads one blob's UTF-8 text at a given path from a proposal commit's tree. Callers only use this
 * for a path already confirmed present by {@link NotebookGitProposalTreeShape}, and the bytes are
 * decoded without re-validating UTF-8 strictness because {@link NotebookGitProposalMarkdownFormat}
 * already confirms every {@code .md} blob in the proposed tree is strictly valid UTF-8 first.
 */
public final class NotebookGitProposalBlobText {

  private NotebookGitProposalBlobText() {}

  public static String readUtf8(Repository repository, ObjectId commitId, String path) {
    try (RevWalk revWalk = new RevWalk(repository)) {
      RevCommit commit = revWalk.parseCommit(commitId);
      try (TreeWalk treeWalk = TreeWalk.forPath(repository, path, commit.getTree())) {
        if (treeWalk == null) {
          throw new IOException("path not found in proposed tree: " + path);
        }
        ObjectLoader loader = repository.open(treeWalk.getObjectId(0));
        return new String(loader.getBytes(), StandardCharsets.UTF_8);
      }
    } catch (IOException e) {
      throw new UncheckedIOException("could not read proposed blob at " + path, e);
    }
  }
}
