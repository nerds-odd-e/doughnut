package com.odde.donut.services.notebookGit;

import com.odde.donut.services.notebookTree.PortableTreeEntry;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.eclipse.jgit.lib.Constants;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.ObjectInserter;

/**
 * A commit's final Portable tree as each path's Git blob id, comparable with {@link
 * NotebookGitAcceptedTree#blobIds}, plus the bytes of the blobs the object store may still lack.
 * Blobs already held by the parent commit need no bytes.
 */
public record NotebookGitTreeContent(Map<String, ObjectId> blobIds, Map<ObjectId, byte[]> blobs) {

  /** Hashes every entry: the whole tree with every blob's bytes. */
  public static NotebookGitTreeContent of(List<PortableTreeEntry> entries) {
    ObjectInserter.Formatter formatter = new ObjectInserter.Formatter();
    Map<String, ObjectId> blobIds = new HashMap<>();
    Map<ObjectId, byte[]> blobs = new HashMap<>();
    for (PortableTreeEntry entry : entries) {
      ObjectId blobId = formatter.idFor(Constants.OBJ_BLOB, entry.content());
      blobIds.put(entry.path(), blobId);
      blobs.put(blobId, entry.content());
    }
    return new NotebookGitTreeContent(blobIds, blobs);
  }
}
