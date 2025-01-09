package com.odde.doughnut.services;

import com.odde.doughnut.entities.Note;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;
import org.springframework.stereotype.Service;

@Service
public class ObsidianExportService {
  public byte[] exportToObsidian(Note headNote) throws IOException {
    try (var baos = new ByteArrayOutputStream();
        var zos = new ZipOutputStream(baos)) {

      writeNoteToZip(headNote, zos, "");

      zos.close();
      return baos.toByteArray();
    }
  }

  private void writeNoteToZip(Note note, ZipOutputStream zos, String path) throws IOException {
    boolean hasChildren = !note.getChildren().isEmpty();
    String sanitizedTopic = sanitizeFileName(note.getTopicConstructor());

    String filePath;
    if (hasChildren) {
      filePath =
          path.isEmpty()
              ? sanitizedTopic + "/__index.md"
              : path + "/" + sanitizedTopic + "/__index.md";
    } else {
      filePath = path.isEmpty() ? sanitizedTopic + ".md" : path + "/" + sanitizedTopic + ".md";
    }

    String fileContent = "# " + note.getTopicConstructor() + "\n" + note.getDetails();
    zos.putNextEntry(new ZipEntry(filePath));
    zos.write(fileContent.getBytes());

    for (Note child : note.getChildren()) {
      writeNoteToZip(child, zos, path.isEmpty() ? sanitizedTopic : path + "/" + sanitizedTopic);
    }
  }

  private String sanitizeFileName(String fileName) {
    return fileName.replaceAll("[\\\\/:*?\"<>|]", "_");
  }
}
