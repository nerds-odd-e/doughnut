package com.odde.donut.services.notebookTree;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Builds the canonical, ordered Portable-tree snapshot for a notebook: one entry per README.md,
 * note file and attachment, in the same traversal order every consumer uses. Within a directory the
 * order is README, then notes, then attachments by filename, then subdirectories.
 */
public final class PortableTreeSnapshot {
  // Notebook/Folder rows are database ids and are never 0, so 0 safely means "no parent / root".
  private static final int ROOT_KEY = 0;

  private final Map<Integer, List<PortableTreeFolderRow>> childFoldersByParent;
  private final Map<Integer, List<PortableTreeNoteRow>> notesByFolder;
  private final Map<Integer, List<PortableTreeAttachmentRow>> attachmentsByFolder;
  private final List<PortableTreeEntry> entries = new ArrayList<>();

  private PortableTreeSnapshot(
      List<PortableTreeFolderRow> folders,
      List<PortableTreeNoteRow> notes,
      List<PortableTreeAttachmentRow> attachments) {
    childFoldersByParent =
        folders.stream().collect(Collectors.groupingBy(f -> folderKey(f.parentFolderId())));
    notesByFolder = notes.stream().collect(Collectors.groupingBy(n -> folderKey(n.folderId())));
    attachmentsByFolder =
        attachments.stream().collect(Collectors.groupingBy(a -> folderKey(a.folderId())));
  }

  public static List<PortableTreeEntry> build(
      String notebookReadmeContent,
      List<PortableTreeFolderRow> folders,
      List<PortableTreeNoteRow> notes,
      List<PortableTreeAttachmentRow> attachments) {
    PortableTreeSnapshot snapshot = new PortableTreeSnapshot(folders, notes, attachments);
    snapshot.collectDirectory("", ROOT_KEY, notebookReadmeContent);
    return snapshot.entries;
  }

  private static int folderKey(Integer folderId) {
    return folderId == null ? ROOT_KEY : folderId;
  }

  private void collectDirectory(String pathPrefix, int folderKey, String readmeContentOrNull) {
    int firstEntryIndex = entries.size();
    if (readmeContentOrNull != null && !readmeContentOrNull.isBlank()) {
      entries.add(
          PortableTreeEntry.ofText(
              pathPrefix + "README.md", PortableTreeReadmeMarkdown.assemble(readmeContentOrNull)));
    }

    for (PortableTreeNoteRow note : notesByFolder.getOrDefault(folderKey, List.of())) {
      entries.add(PortableTreeEntry.ofNote(pathPrefix + note.title() + ".md", note.content()));
    }

    List<PortableTreeAttachmentRow> orderedAttachments =
        attachmentsByFolder.getOrDefault(folderKey, List.of()).stream()
            .sorted(Comparator.comparing(PortableTreeAttachmentRow::filename))
            .toList();
    for (PortableTreeAttachmentRow attachment : orderedAttachments) {
      entries.add(new PortableTreeEntry(pathPrefix + attachment.filename(), attachment.content()));
    }

    for (PortableTreeFolderRow folder : childFoldersByParent.getOrDefault(folderKey, List.of())) {
      collectDirectory(pathPrefix + folder.name() + "/", folder.id(), folder.readmeContent());
    }

    if (!pathPrefix.isEmpty() && entries.size() == firstEntryIndex) {
      entries.add(PortableTreeEntry.ofText(pathPrefix + ".keep", ""));
    }
  }
}
