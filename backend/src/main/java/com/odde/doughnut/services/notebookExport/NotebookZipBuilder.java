package com.odde.doughnut.services.notebookExport;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

public final class NotebookZipBuilder {
  // Notebook/Folder rows are database ids and are never 0, so 0 safely means "no parent / root".
  private static final int ROOT_KEY = 0;

  private NotebookZipBuilder() {}

  public static byte[] build(
      String notebookReadmeContent, List<ExportFolderRow> folders, List<ExportNoteRow> notes) {
    Map<Integer, List<ExportFolderRow>> childFoldersByParent =
        folders.stream()
            .collect(
                Collectors.groupingBy(
                    f -> f.parentFolderId() == null ? ROOT_KEY : f.parentFolderId()));
    Map<Integer, List<ExportNoteRow>> notesByFolder =
        notes.stream()
            .collect(Collectors.groupingBy(n -> n.folderId() == null ? ROOT_KEY : n.folderId()));

    try {
      ByteArrayOutputStream baos = new ByteArrayOutputStream();
      try (ZipOutputStream zos = new ZipOutputStream(baos)) {
        writeDirectory(
            zos,
            "",
            notebookReadmeContent,
            childFoldersByParent.getOrDefault(ROOT_KEY, List.of()),
            notesByFolder.getOrDefault(ROOT_KEY, List.of()),
            childFoldersByParent,
            notesByFolder);
      }
      return baos.toByteArray();
    } catch (IOException e) {
      throw new UncheckedIOException(e);
    }
  }

  private static void writeDirectory(
      ZipOutputStream zos,
      String pathPrefix,
      String readmeContentOrNull,
      List<ExportFolderRow> childFolders,
      List<ExportNoteRow> notesHere,
      Map<Integer, List<ExportFolderRow>> childFoldersByParent,
      Map<Integer, List<ExportNoteRow>> notesByFolder)
      throws IOException {
    if (readmeContentOrNull != null && !readmeContentOrNull.isBlank()) {
      writeEntry(zos, pathPrefix + "README.md", ExportReadmeMarkdown.assemble(readmeContentOrNull));
    }

    Map<Integer, String> noteFileNames =
        NotebookExportFilenames.uniqueFileNames(
            notesHere.stream().map(n -> Map.entry(n.id(), n.title())).toList(), ".md");
    for (ExportNoteRow note : notesHere) {
      writeEntry(zos, pathPrefix + noteFileNames.get(note.id()), ExportNoteMarkdown.assemble(note));
    }

    Map<Integer, String> folderDirNames =
        NotebookExportFilenames.uniqueFileNames(
            childFolders.stream().map(f -> Map.entry(f.id(), f.name())).toList(), "");
    for (ExportFolderRow folder : childFolders) {
      String subPath = pathPrefix + folderDirNames.get(folder.id()) + "/";
      writeDirectory(
          zos,
          subPath,
          folder.readmeContent(),
          childFoldersByParent.getOrDefault(folder.id(), List.of()),
          notesByFolder.getOrDefault(folder.id(), List.of()),
          childFoldersByParent,
          notesByFolder);
    }
  }

  private static void writeEntry(ZipOutputStream zos, String path, String content)
      throws IOException {
    zos.putNextEntry(new ZipEntry(path));
    zos.write(content.getBytes(StandardCharsets.UTF_8));
    zos.closeEntry();
  }
}
