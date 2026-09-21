package com.odde.donut.services.notebookExport;

import com.odde.donut.services.notebookTree.PortableTreeEntry;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

/** Writes an ordered Portable tree out as a ZIP archive, one entry per tree entry. */
public final class NotebookZipBuilder {

  private NotebookZipBuilder() {}

  public static byte[] build(List<PortableTreeEntry> entries) {
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

  private static void writeEntry(ZipOutputStream zos, String path, byte[] content)
      throws IOException {
    zos.putNextEntry(new ZipEntry(path));
    zos.write(content);
    zos.closeEntry();
  }
}
