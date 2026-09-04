package com.odde.donut.services.notebookExport;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

public final class NotebookZipBuilder {

  private NotebookZipBuilder() {}

  public static byte[] build(
      String notebookReadmeContent, List<ExportFolderRow> folders, List<ExportNoteRow> notes) {
    List<PortableTreeEntry> entries =
        PortableTreeSnapshot.build(notebookReadmeContent, folders, notes);

    try {
      ByteArrayOutputStream baos = new ByteArrayOutputStream();
      try (ZipOutputStream zos = new ZipOutputStream(baos)) {
        for (PortableTreeEntry entry : entries) {
          writeEntry(zos, entry.path(), entry.content());
        }
      }
      return baos.toByteArray();
    } catch (IOException e) {
      throw new UncheckedIOException(e);
    }
  }

  private static void writeEntry(ZipOutputStream zos, String path, String content)
      throws IOException {
    zos.putNextEntry(new ZipEntry(path));
    zos.write(content.getBytes(StandardCharsets.UTF_8));
    zos.closeEntry();
  }
}
