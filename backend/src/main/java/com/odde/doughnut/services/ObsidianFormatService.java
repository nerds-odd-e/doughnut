package com.odde.doughnut.services;

import com.odde.doughnut.entities.Note;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;
import org.springframework.stereotype.Service;

@Service
public class ObsidianFormatService {
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
    String filePath = generateFilePath(path, note.getTopicConstructor(), hasChildren);
    String fileContent = generateMarkdownContent(note);
    zos.putNextEntry(new ZipEntry(filePath));
    zos.write(fileContent.getBytes());

    for (Note child : note.getChildren()) {
      String newPath = path.isEmpty() ? note.getTopicConstructor() : path + "/" + note.getTopicConstructor();
      writeNoteToZip(child, zos, newPath);
    }
  }

  private String generateFilePath(String path, String topic, boolean hasChildren) {
    String sanitizedTopic = sanitizeFileName(topic);
    String fileName = hasChildren ? sanitizedTopic + "/__index.md" : sanitizedTopic + ".md";
    return path.isEmpty() ? fileName : sanitizeFileName(path) + "/" + fileName;
  }

  private String generateMarkdownContent(Note note) {
    return "# " + note.getTopicConstructor() + "\n" + note.getDetails();
  }

  private String sanitizeFileName(String fileName) {
    return fileName.replaceAll("[\\\\/:*?\"<>|]", "_");
  }
}
