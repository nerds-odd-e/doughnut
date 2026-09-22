package com.odde.donut.services.notebookGit;

import java.io.IOException;
import java.io.UncheckedIOException;
import org.eclipse.jgit.lib.FileMode;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.ObjectReader;
import org.eclipse.jgit.treewalk.CanonicalTreeParser;

/** Loads one accepted tree object into a mutable {@link NotebookGitDirectoryTree}. */
final class NotebookGitAcceptedDirectoryReader {
  private final ObjectReader objectReader;

  NotebookGitAcceptedDirectoryReader(ObjectReader objectReader) {
    this.objectReader = objectReader;
  }

  NotebookGitDirectoryTree open(ObjectId treeId) {
    try {
      CanonicalTreeParser parser = new CanonicalTreeParser();
      parser.reset(objectReader, treeId);
      NotebookGitDirectoryTree tree = new NotebookGitDirectoryTree(this);
      while (!parser.eof()) {
        String name = parser.getEntryPathString();
        ObjectId id = parser.getEntryObjectId();
        if (FileMode.TREE.equals(parser.getEntryFileMode())) {
          tree.putLoaded(name, new NotebookGitDirectoryTree.Entry.TreeRef(id.copy()));
        } else {
          tree.putLoaded(
              name, new NotebookGitDirectoryTree.Entry.File(id.copy(), parser.getEntryFileMode()));
        }
        parser.next();
      }
      return tree;
    } catch (IOException e) {
      throw new UncheckedIOException("Could not read accepted directory tree", e);
    }
  }
}
