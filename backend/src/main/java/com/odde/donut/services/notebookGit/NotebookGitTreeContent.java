package com.odde.donut.services.notebookGit;

import com.odde.donut.services.notebookTree.PortableTreeEntry;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.eclipse.jgit.lib.Constants;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.ObjectInserter;

/**
 * A commit's final Portable tree as its native root tree id, the bytes of objects the store may
 * still lack (changed blobs and newly written trees), and — when the tree was fully assembled —
 * each path's blob id for projection drift checks.
 */
public record NotebookGitTreeContent(
    ObjectId treeId,
    Map<String, ObjectId> blobIds,
    Map<ObjectId, byte[]> blobs,
    Map<ObjectId, byte[]> trees) {

  /** Hashes every entry and builds the native root from those path blobs. */
  public static NotebookGitTreeContent of(List<PortableTreeEntry> entries) {
    NotebookGitDirectoryTree root = new NotebookGitDirectoryTree();
    Map<ObjectId, byte[]> blobs = new HashMap<>();
    entries.forEach(entry -> putEntry(entry, root, blobs));
    return fromDirectory(root, blobs);
  }

  /**
   * Places {@code entry} at its path in {@code tree}, recording its blob bytes in {@code blobs}.
   */
  static void putEntry(
      PortableTreeEntry entry, NotebookGitDirectoryTree tree, Map<ObjectId, byte[]> blobs) {
    ObjectId blobId = new ObjectInserter.Formatter().idFor(Constants.OBJ_BLOB, entry.content());
    blobs.put(blobId, entry.content());
    tree.putFile(entry.path(), blobId);
  }

  /**
   * Native trees from an edited or fully assembled directory. Derived edits with unresolved child
   * refs leave {@code blobIds} empty; complete assemblies flatten every path.
   */
  static NotebookGitTreeContent fromDirectory(
      NotebookGitDirectoryTree tree, Map<ObjectId, byte[]> blobs) {
    Map<ObjectId, byte[]> trees = new HashMap<>();
    ObjectId treeId = tree.writeTrees(trees);
    Map<String, ObjectId> blobIds = tree.hasUnresolvedTreeRefs() ? Map.of() : tree.toBlobIds();
    return new NotebookGitTreeContent(treeId, blobIds, blobs, trees);
  }
}
