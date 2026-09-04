package com.odde.donut.services.notebookExport;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Builds the canonical, ordered Portable-tree snapshot for a notebook export: one entry per
 * README.md and note file, in the same traversal order the ZIP export produces.
 */
public final class PortableTreeSnapshot {
  // Notebook/Folder rows are database ids and are never 0, so 0 safely means "no parent / root".
  private static final int ROOT_KEY = 0;

  private PortableTreeSnapshot() {}

  public static List<PortableTreeEntry> build(
      String notebookReadmeContent, List<ExportFolderRow> folders, List<ExportNoteRow> notes) {
    Map<Integer, List<ExportFolderRow>> childFoldersByParent =
        folders.stream()
            .collect(
                Collectors.groupingBy(
                    f -> f.parentFolderId() == null ? ROOT_KEY : f.parentFolderId()));
    Map<Integer, List<ExportNoteRow>> notesByFolder =
        notes.stream()
            .collect(Collectors.groupingBy(n -> n.folderId() == null ? ROOT_KEY : n.folderId()));

    List<PortableTreeEntry> entries = new ArrayList<>();
    collectDirectory(
        entries,
        "",
        notebookReadmeContent,
        childFoldersByParent.getOrDefault(ROOT_KEY, List.of()),
        notesByFolder.getOrDefault(ROOT_KEY, List.of()),
        childFoldersByParent,
        notesByFolder);
    return entries;
  }

  private static void collectDirectory(
      List<PortableTreeEntry> entries,
      String pathPrefix,
      String readmeContentOrNull,
      List<ExportFolderRow> childFolders,
      List<ExportNoteRow> notesHere,
      Map<Integer, List<ExportFolderRow>> childFoldersByParent,
      Map<Integer, List<ExportNoteRow>> notesByFolder) {
    if (readmeContentOrNull != null && !readmeContentOrNull.isBlank()) {
      entries.add(
          new PortableTreeEntry(
              pathPrefix + "README.md", ExportReadmeMarkdown.assemble(readmeContentOrNull)));
    }

    for (ExportNoteRow note : notesHere) {
      String content = note.content() == null ? "" : note.content();
      entries.add(new PortableTreeEntry(pathPrefix + note.title() + ".md", content));
    }

    for (ExportFolderRow folder : childFolders) {
      String subPath = pathPrefix + folder.name() + "/";
      collectDirectory(
          entries,
          subPath,
          folder.readmeContent(),
          childFoldersByParent.getOrDefault(folder.id(), List.of()),
          notesByFolder.getOrDefault(folder.id(), List.of()),
          childFoldersByParent,
          notesByFolder);
    }
  }
}
