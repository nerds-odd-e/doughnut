package com.odde.doughnut.services;

import com.odde.doughnut.entities.Note;
import com.odde.doughnut.entities.Notebook;
import com.odde.doughnut.entities.User;
import com.odde.doughnut.factoryServices.ModelFactoryService;
import com.odde.doughnut.testability.TestabilitySettings;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;
import org.springframework.web.multipart.MultipartFile;

public class ObsidianFormatService {

  private final NoteConstructionService noteConstructionService;
  private final ModelFactoryService modelFactoryService;

  public ObsidianFormatService(User user, ModelFactoryService modelFactoryService) {
    TestabilitySettings testabilitySettings = new TestabilitySettings();
    this.modelFactoryService = modelFactoryService;
    noteConstructionService =
        new NoteConstructionService(
            user, testabilitySettings.getCurrentUTCTimestamp(), this.modelFactoryService);
  }

  public byte[] exportToObsidian(Note headNote) throws IOException {
    try (var baos = new ByteArrayOutputStream();
        var zos = new ZipOutputStream(baos)) {

      writeNoteToZip(headNote, zos, "");

      zos.close();
      return baos.toByteArray();
    }
  }

  private void writeNoteToZip(Note note, ZipOutputStream zos, String path) throws IOException {
    String filePath = generateFilePath(path, note);
    String fileContent = generateMarkdownContent(note);
    zos.putNextEntry(new ZipEntry(filePath));
    zos.write(fileContent.getBytes());

    for (Note child : note.getChildren()) {
      String newPath =
          path.isEmpty() ? note.getTopicConstructor() : path + "/" + note.getTopicConstructor();
      writeNoteToZip(child, zos, newPath);
    }
  }

  private String generateFilePath(String path, Note note) {
    String sanitizedTopic = sanitizeFileName(note.getTopicConstructor());
    String fileName =
        note.getChildren().isEmpty() ? sanitizedTopic + ".md" : sanitizedTopic + "/__index.md";
    return path.isEmpty() ? fileName : path + "/" + fileName;
  }

  private String generateMarkdownContent(Note note) {
    return "# " + note.getTopicConstructor() + "\n" + note.getDetails();
  }

  private String sanitizeFileName(String fileName) {
    return fileName.replaceAll("[\\\\/:*?\"<>|]", "_");
  }

  public void importFromObsidian(MultipartFile file, Notebook notebook) throws IOException {

    try (ZipInputStream zipIn = new ZipInputStream(file.getInputStream())) {
      ZipEntry entry;

      while ((entry = zipIn.getNextEntry()) != null) {
        Note currentParent = notebook.getHeadNote();
        String entryName = entry.getName();

        if (entryName.startsWith(".") || entryName.contains("/.")) {
          continue;
        }

        String[] pathParts = entryName.split("/");

        // Create notes for each directory in the path
        for (int i = 1; i < pathParts.length; i++) {
          String part = pathParts[i];

          // Skip empty parts and .md extension
          if (part.isEmpty() || part.equals(".md")) {
            continue;
          }

          // Remove .md extension if it's a file
          if (part.endsWith(".md")) {
            part = part.substring(0, part.length() - 3);
          }

          // Check if note already exists under current parent
          String finalPart = part;
          Note existingNote =
              currentParent.getChildren().stream()
                  .filter(note -> note.getNoteTitle().matches(finalPart))
                  .findFirst()
                  .orElse(null);

          if (existingNote == null) {
            // Create new note
            Note newNote = noteConstructionService.createNote(currentParent, finalPart);
            if (!entry.isDirectory() && i == pathParts.length - 1) {
              newNote.prependDescription(new String(zipIn.readAllBytes()));
              modelFactoryService.save(newNote);
            }
            currentParent = newNote;
          } else {
            currentParent = existingNote;
          }
        }
      }
    }
  }
}
