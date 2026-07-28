package com.odde.doughnut.services.notebookExport;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

public final class NotebookZipBuilder {
  private NotebookZipBuilder() {}

  public static byte[] build(
      String notebookReadmeContent, List<ExportFolderRow> folders, List<ExportNoteRow> notes) {
    try {
      ByteArrayOutputStream baos = new ByteArrayOutputStream();
      try (ZipOutputStream zos = new ZipOutputStream(baos)) {
        if (notebookReadmeContent != null && !notebookReadmeContent.isBlank()) {
          writeEntry(zos, "README.md", notebookReadmeContent);
        }
        Map<Integer, String> noteFileNames =
            NotebookExportFilenames.uniqueFileNames(
                notes.stream().map(n -> Map.entry(n.id(), n.title())).toList(), ".md");
        for (ExportNoteRow note : notes) {
          writeEntry(zos, noteFileNames.get(note.id()), noteFileContent(note));
        }
      }
      return baos.toByteArray();
    } catch (IOException e) {
      throw new UncheckedIOException(e);
    }
  }

  private static String noteFileContent(ExportNoteRow note) {
    return "# " + note.title() + "\n\n" + (note.content() == null ? "" : note.content());
  }

  private static void writeEntry(ZipOutputStream zos, String path, String content)
      throws IOException {
    zos.putNextEntry(new ZipEntry(path));
    zos.write(content.getBytes(StandardCharsets.UTF_8));
    zos.closeEntry();
  }
}
