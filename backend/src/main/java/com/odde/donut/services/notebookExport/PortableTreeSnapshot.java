package com.odde.donut.services.notebookExport;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Builds the canonical, ordered Portable-tree snapshot for a notebook export: one entry per
 * README.md, note file and root attachment, in the same traversal order the ZIP export produces.
 * Within a directory the order is README, then notes, then attachments by filename, then
 * subdirectories.
 */
public final class PortableTreeSnapshot {
  // Notebook/Folder rows are database ids and are never 0, so 0 safely means "no parent / root".
  private static final int ROOT_KEY = 0;

  private final Map<Integer, List<ExportFolderRow>> childFoldersByParent;
  private final Map<Integer, List<ExportNoteRow>> notesByFolder;
  private final List<PortableTreeEntry> entries = new ArrayList<>();

  private PortableTreeSnapshot(List<ExportFolderRow> folders, List<ExportNoteRow> notes) {
    childFoldersByParent =
        folders.stream().collect(Collectors.groupingBy(f -> folderKey(f.parentFolderId())));
    notesByFolder = notes.stream().collect(Collectors.groupingBy(n -> folderKey(n.folderId())));
  }

  public static List<PortableTreeEntry> build(
      String notebookReadmeContent,
      List<ExportFolderRow> folders,
      List<ExportNoteRow> notes,
      List<ExportAttachmentRow> rootAttachments) {
    PortableTreeSnapshot snapshot = new PortableTreeSnapshot(folders, notes);
    snapshot.collectDirectory("", ROOT_KEY, notebookReadmeContent, rootAttachments);
    return snapshot.entries;
  }

  private static int folderKey(Integer folderId) {
    return folderId == null ? ROOT_KEY : folderId;
  }

  private void collectDirectory(
      String pathPrefix,
      int folderKey,
      String readmeContentOrNull,
      List<ExportAttachmentRow> attachmentsHere) {
    int firstEntryIndex = entries.size();
    if (readmeContentOrNull != null && !readmeContentOrNull.isBlank()) {
      entries.add(
          PortableTreeEntry.ofText(
              pathPrefix + "README.md", ExportReadmeMarkdown.assemble(readmeContentOrNull)));
    }

    for (ExportNoteRow note : notesByFolder.getOrDefault(folderKey, List.of())) {
      String content = note.content() == null ? "" : note.content();
      entries.add(PortableTreeEntry.ofText(pathPrefix + note.title() + ".md", content));
    }

    List<ExportAttachmentRow> orderedAttachments =
        attachmentsHere.stream()
            .sorted(Comparator.comparing(ExportAttachmentRow::filename))
            .toList();
    for (ExportAttachmentRow attachment : orderedAttachments) {
      entries.add(new PortableTreeEntry(pathPrefix + attachment.filename(), attachment.content()));
    }

    for (ExportFolderRow folder : childFoldersByParent.getOrDefault(folderKey, List.of())) {
      collectDirectory(
          pathPrefix + folder.name() + "/", folder.id(), folder.readmeContent(), List.of());
    }

    if (!pathPrefix.isEmpty() && entries.size() == firstEntryIndex) {
      entries.add(PortableTreeEntry.ofText(pathPrefix + ".keep", ""));
    }
  }
}
