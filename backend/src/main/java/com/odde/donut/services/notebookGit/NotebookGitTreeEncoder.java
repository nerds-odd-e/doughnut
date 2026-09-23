package com.odde.donut.services.notebookGit;

import com.odde.donut.entities.Note;
import com.odde.donut.entities.Notebook;
import com.odde.donut.entities.repositories.FolderRepository;
import com.odde.donut.entities.repositories.NoteRepository;
import com.odde.donut.entities.repositories.NotebookAttachmentRepository;
import com.odde.donut.entities.repositories.NotebookRepository;
import com.odde.donut.services.notebookGit.ProjectionChangeCapture.NotebookProjectionChange;
import com.odde.donut.services.notebookTree.PortableTreeAttachmentRow;
import com.odde.donut.services.notebookTree.PortableTreeEntry;
import com.odde.donut.services.notebookTree.PortableTreeFolderRow;
import com.odde.donut.services.notebookTree.PortableTreeNoteRow;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;
import org.eclipse.jgit.lib.Constants;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.ObjectInserter;
import org.eclipse.jgit.lib.ObjectReader;
import org.eclipse.jgit.lib.Repository;
import org.springframework.stereotype.Component;

/**
 * The one encoder of projection rows into a commit's Portable tree. A web commit's tree is derived
 * by editing the accepted directory tree with the projection change the operation flushed; a
 * notebook's full tree (cutover, history reset, the publication drift check) is the same encoding
 * over an empty base with every row as an insertion.
 */
@Component
class NotebookGitTreeEncoder {
  private final NoteRepository noteRepository;
  private final FolderRepository folderRepository;
  private final NotebookRepository notebookRepository;
  private final NotebookAttachmentRepository notebookAttachmentRepository;

  NotebookGitTreeEncoder(
      NoteRepository noteRepository,
      FolderRepository folderRepository,
      NotebookRepository notebookRepository,
      NotebookAttachmentRepository notebookAttachmentRepository) {
    this.noteRepository = noteRepository;
    this.folderRepository = folderRepository;
    this.notebookRepository = notebookRepository;
    this.notebookAttachmentRepository = notebookAttachmentRepository;
  }

  /**
   * Accepted tree edited by the projection change: stale file paths cleared, folder subtrees
   * relocated, note blobs and container readmes refreshed, {@code .keep} re-evaluated.
   */
  NotebookGitTreeContent derive(
      NotebookProjectionChange change, Repository repository, ObjectId acceptedRootTreeId) {
    NotebookGitChangedFolders folders = new NotebookGitChangedFolders(folderRepository, change);
    NotebookGitChangedFiles files =
        new NotebookGitChangedFiles(noteRepository, notebookAttachmentRepository, change);
    Set<String> touchedDirectories = new LinkedHashSet<>(folders.touchedDirectories());
    try (ObjectReader objectReader = repository.newObjectReader()) {
      NotebookGitDirectoryTree tree =
          NotebookGitDirectoryTree.fromAcceptedRoot(objectReader, acceptedRootTreeId);
      files.forgetStalePaths(tree, folders, touchedDirectories);
      folders.relocate(tree);
      folders.currentFolders().keySet().forEach(tree::ensureDirectory);
      return encode(
          tree, files.currentNoteEntries(), readmeContents(change, folders), touchedDirectories);
    }
  }

  /** The notebook's whole tree from its stored folders, notes (trash included) and attachments. */
  NotebookGitTreeContent fullTree(Notebook notebook) {
    return fullTree(notebook, List.of());
  }

  /**
   * Full assembly that preserves accepted Git metadata (for example {@code .gitattributes}) rather
   * than regenerating it from defaults.
   */
  NotebookGitTreeContent fullTree(Notebook notebook, List<PortableTreeEntry> acceptedMetadata) {
    return fullTree(
        notebook.getReadmeContent(),
        folderRepository.findPortableTreeRowsByNotebookId(notebook.getId()),
        noteRepository.findPortableTreeRowsByNotebookId(notebook.getId()),
        notebookAttachmentRepository.findPortableTreeRowsByNotebookId(notebook.getId()),
        acceptedMetadata);
  }

  /** The same tree from folders and notes a caller already holds, as a locked publication does. */
  NotebookGitTreeContent fullTree(
      Notebook notebook, List<PortableTreeFolderRow> folders, List<Note> storedNotes) {
    return fullTree(notebook, folders, storedNotes, List.of());
  }

  NotebookGitTreeContent fullTree(
      Notebook notebook,
      List<PortableTreeFolderRow> folders,
      List<Note> storedNotes,
      List<PortableTreeEntry> acceptedMetadata) {
    return fullTree(
        notebook.getReadmeContent(),
        folders,
        storedNotes.stream()
            .map(
                note ->
                    new PortableTreeNoteRow(
                        note.getFolder() == null ? null : note.getFolder().getId(),
                        note.getTitle(),
                        note.getContent()))
            .toList(),
        notebookAttachmentRepository.findPortableTreeRowsByNotebookId(notebook.getId()),
        acceptedMetadata);
  }

  /** Every row as an insertion over an empty base. */
  static NotebookGitTreeContent fullTree(
      String notebookReadmeContent,
      List<PortableTreeFolderRow> folders,
      List<PortableTreeNoteRow> notes,
      List<PortableTreeAttachmentRow> attachments) {
    return fullTree(notebookReadmeContent, folders, notes, attachments, List.of());
  }

  /**
   * Every projection row as an insertion over an empty base, plus reserved Git metadata preserved
   * from the accepted tip (or supplied for LFS initialization).
   */
  static NotebookGitTreeContent fullTree(
      String notebookReadmeContent,
      List<PortableTreeFolderRow> folders,
      List<PortableTreeNoteRow> notes,
      List<PortableTreeAttachmentRow> attachments,
      List<PortableTreeEntry> acceptedMetadata) {
    Map<Integer, String> prefixes = NotebookGitPortablePath.folderPrefixes(folders);
    Map<String, String> readmeContents = new HashMap<>();
    readmeContents.put("", notebookReadmeContent);
    folders.stream()
        .filter(folder -> prefixes.containsKey(folder.id()))
        .forEach(folder -> readmeContents.put(prefixes.get(folder.id()), folder.readmeContent()));
    List<PortableTreeEntry> files =
        Stream.concat(
                Stream.concat(
                    notes.stream()
                        .filter(note -> prefixes.containsKey(note.folderId()))
                        .map(
                            note ->
                                PortableTreeEntry.ofNote(
                                    NotebookGitPortablePath.ofNote(
                                        prefixes.get(note.folderId()), note.title()),
                                    note.content())),
                    attachments.stream()
                        .filter(attachment -> prefixes.containsKey(attachment.folderId()))
                        .map(
                            attachment ->
                                new PortableTreeEntry(
                                    NotebookGitPortablePath.ofAttachment(
                                        prefixes.get(attachment.folderId()), attachment.filename()),
                                    attachment.acceptedGitContent()))),
                acceptedMetadata.stream())
            .toList();
    Set<String> retainedDirectories = new LinkedHashSet<>();
    prefixes.values().stream()
        .filter(prefix -> !prefix.isEmpty())
        .forEach(retainedDirectories::add);
    return encode(new NotebookGitDirectoryTree(), files, readmeContents, retainedDirectories);
  }

  /**
   * Puts each file, refreshes each container's {@code README.md}, and re-evaluates {@code .keep}
   * deepest first. Serialization emits native trees that reuse unresolved child tree ids.
   */
  private static NotebookGitTreeContent encode(
      NotebookGitDirectoryTree tree,
      List<PortableTreeEntry> files,
      Map<String, String> readmeContents,
      Set<String> touchedDirectories) {
    Map<ObjectId, byte[]> blobs = new HashMap<>();
    Set<String> directories = new LinkedHashSet<>(touchedDirectories);
    files.forEach(
        file -> {
          put(file, tree, blobs);
          directories.add(NotebookGitPortablePath.directoryOf(file.path()));
        });
    readmeContents.forEach(
        (prefix, content) -> {
          tree.removeFile(PortableTreeEntry.readmePath(prefix));
          PortableTreeEntry.ofReadme(prefix, content).ifPresent(entry -> put(entry, tree, blobs));
          directories.add(prefix);
        });
    directories.stream().filter(directory -> !directory.isEmpty()).forEach(tree::ensureDirectory);
    normalizeKeep(tree, directories, blobs);
    return NotebookGitTreeContent.fromDirectory(tree, blobs);
  }

  /**
   * Deepest-first: a represented directory with no entries other than {@code .keep} holds {@code
   * .keep}; one with other content drops it. The notebook root never gets {@code .keep}.
   */
  private static void normalizeKeep(
      NotebookGitDirectoryTree tree, Set<String> directories, Map<ObjectId, byte[]> blobs) {
    directories.stream()
        .filter(directory -> !directory.isEmpty())
        .sorted((a, b) -> Integer.compare(b.length(), a.length()))
        .forEach(
            directory -> {
              if (tree.isEmptyAsideFromKeep(directory)) {
                put(PortableTreeEntry.ofText(directory + ".keep", ""), tree, blobs);
              } else {
                tree.removeFile(directory + ".keep");
              }
            });
  }

  /**
   * The current readme content of every container whose row changed, by its current prefix: each
   * inserted or updated folder, and the notebook root when its readme changed.
   */
  private Map<String, String> readmeContents(
      NotebookProjectionChange change, NotebookGitChangedFolders folders) {
    Map<String, String> contents = new HashMap<>();
    folders
        .currentFolders()
        .forEach((prefix, folder) -> contents.put(prefix, folder.getReadmeContent()));
    change.updated.keySet().stream()
        .filter(row -> row.kind() == Notebook.class)
        .findAny()
        .ifPresent(
            row ->
                contents.put(
                    "", notebookRepository.findById(row.id()).orElseThrow().getReadmeContent()));
    return contents;
  }

  private static void put(
      PortableTreeEntry entry, NotebookGitDirectoryTree tree, Map<ObjectId, byte[]> blobs) {
    ObjectId blobId = new ObjectInserter.Formatter().idFor(Constants.OBJ_BLOB, entry.content());
    blobs.put(blobId, entry.content());
    tree.putFile(entry.path(), blobId);
  }
}
