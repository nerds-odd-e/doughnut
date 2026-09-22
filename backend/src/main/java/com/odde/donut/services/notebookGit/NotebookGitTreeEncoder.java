package com.odde.donut.services.notebookGit;

import com.odde.donut.entities.Folder;
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
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;
import org.eclipse.jgit.lib.ObjectId;
import org.springframework.stereotype.Component;

/**
 * The one encoder of projection rows into a commit's Portable tree. A web commit's tree is derived
 * from the accepted head's path-to-blob map and the projection change the operation flushed, so
 * only the changed rows are rendered and hashed; a notebook's full tree (cutover, history reset,
 * the publication drift check) is the same encoding over an empty base with every row as an
 * insertion.
 */
@Component
public class NotebookGitTreeEncoder {
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
   * The accepted tree with every changed note's and attachment's previous path removed, every entry
   * under a changed folder re-listed at its current path with its accepted blob, each inserted or
   * updated note's blob put at its current path, each changed container's {@code README.md}
   * refreshed at its current prefix and the {@code .keep} marker of every touched directory
   * re-evaluated at its current path.
   */
  NotebookGitTreeContent derive(
      NotebookProjectionChange change, Map<String, ObjectId> acceptedBlobIds) {
    NotebookGitChangedFolders folders = new NotebookGitChangedFolders(folderRepository, change);
    Set<String> touchedDirectories = new LinkedHashSet<>(folders.touchedDirectories());

    Map<String, ObjectId> previousTree = new HashMap<>(acceptedBlobIds);
    Stream.concat(change.deleted.entrySet().stream(), change.updated.entrySet().stream())
        .filter(entry -> isFile(entry.getKey().kind()))
        .map(entry -> folders.previousPathOf(entry.getKey().kind(), entry.getValue()))
        .forEach(
            previousPath -> {
              previousTree.remove(previousPath);
              folders
                  .currentPathOf(NotebookGitPortablePath.directoryOf(previousPath))
                  .ifPresent(touchedDirectories::add);
            });
    return encode(
        folders.relist(previousTree),
        currentNoteEntries(change),
        readmeContents(change, folders),
        touchedDirectories);
  }

  /** The notebook's whole tree from its stored folders, notes (trash included) and attachments. */
  public NotebookGitTreeContent fullTree(Notebook notebook) {
    return fullTree(
        notebook.getReadmeContent(),
        folderRepository.findPortableTreeRowsByNotebookId(notebook.getId()),
        noteRepository.findPortableTreeRowsByNotebookId(notebook.getId()),
        notebookAttachmentRepository.findPortableTreeRowsByNotebookId(notebook.getId()));
  }

  /** The same tree from folders and notes a caller already holds, as a locked publication does. */
  public NotebookGitTreeContent fullTree(
      Notebook notebook, List<PortableTreeFolderRow> folders, List<Note> storedNotes) {
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
        notebookAttachmentRepository.findPortableTreeRowsByNotebookId(notebook.getId()));
  }

  /** Every row as an insertion over an empty base. */
  static NotebookGitTreeContent fullTree(
      String notebookReadmeContent,
      List<PortableTreeFolderRow> folders,
      List<PortableTreeNoteRow> notes,
      List<PortableTreeAttachmentRow> attachments) {
    Map<Integer, String> prefixes = NotebookGitPortablePath.folderPrefixes(folders);
    Map<String, String> readmeContents = new HashMap<>();
    readmeContents.put("", notebookReadmeContent);
    folders.stream()
        .filter(folder -> prefixes.containsKey(folder.id()))
        .forEach(folder -> readmeContents.put(prefixes.get(folder.id()), folder.readmeContent()));
    List<PortableTreeEntry> files =
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
                                prefixes.get(attachment.folderId()) + attachment.filename(),
                                attachment.content())))
            .toList();
    return encode(new HashMap<>(), files, readmeContents, Set.of());
  }

  /**
   * The tree with each file put at its path, each container's {@code README.md} following its
   * readme content, and the {@code .keep} marker of every touched directory re-evaluated deepest
   * first: a represented folder with no other entry holds {@code .keep}; the notebook root never
   * does.
   */
  private static NotebookGitTreeContent encode(
      Map<String, ObjectId> blobIds,
      List<PortableTreeEntry> files,
      Map<String, String> readmeContents,
      Set<String> touchedDirectories) {
    Map<ObjectId, byte[]> blobs = new HashMap<>();
    Set<String> directories = new LinkedHashSet<>(touchedDirectories);
    files.forEach(
        file -> {
          put(file, blobIds, blobs);
          directories.add(NotebookGitPortablePath.directoryOf(file.path()));
        });
    readmeContents.forEach(
        (prefix, content) -> {
          blobIds.remove(PortableTreeEntry.readmePath(prefix));
          PortableTreeEntry.ofReadme(prefix, content)
              .ifPresent(entry -> put(entry, blobIds, blobs));
          directories.add(prefix);
        });
    directories.stream()
        .filter(directory -> !directory.isEmpty())
        .sorted(Comparator.comparing(String::length).reversed())
        .forEach(
            directory -> {
              String keep = directory + ".keep";
              boolean empty =
                  blobIds.keySet().stream()
                      .noneMatch(path -> path.startsWith(directory) && !path.equals(keep));
              if (empty) put(PortableTreeEntry.ofText(keep, ""), blobIds, blobs);
              else blobIds.remove(keep);
            });
    return new NotebookGitTreeContent(blobIds, blobs);
  }

  private static boolean isFile(Class<?> kind) {
    return kind != Folder.class && kind != Notebook.class;
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

  /** The current file of every note the change updated or inserted. */
  private List<PortableTreeEntry> currentNoteEntries(NotebookProjectionChange change) {
    Stream<Note> updated =
        change.updated.keySet().stream()
            .filter(row -> row.kind() == Note.class)
            .map(row -> noteRepository.findById(row.id()).orElseThrow());
    Stream<Note> inserted =
        change.inserted.stream().filter(Note.class::isInstance).map(Note.class::cast);
    return Stream.concat(updated, inserted)
        .map(
            note ->
                PortableTreeEntry.ofNote(NotebookGitPortablePath.ofNote(note), note.getContent()))
        .toList();
  }

  private static void put(
      PortableTreeEntry entry, Map<String, ObjectId> blobIds, Map<ObjectId, byte[]> blobs) {
    NotebookGitTreeContent hashed = NotebookGitTreeContent.of(List.of(entry));
    blobIds.putAll(hashed.blobIds());
    blobs.putAll(hashed.blobs());
  }
}
