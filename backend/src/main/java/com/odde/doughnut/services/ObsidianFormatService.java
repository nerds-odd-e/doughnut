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
    return generateFrontMatter(note) + generateNoteContent(note);
  }

  private String generateFrontMatter(Note note) {
    return """
           ---
           note_id: %d
           created_at: %s
           updated_at: %s
           ---
           """
        .formatted(note.getId(), note.getCreatedAt(), note.getUpdatedAt());
  }

  private String generateNoteContent(Note note) {
    return """
           # %s
           %s"""
        .formatted(note.getTopicConstructor(), note.getDetails());
  }

  private String sanitizeFileName(String fileName) {
    return fileName.replaceAll("[\\\\/:*?\"<>|]", "_");
  }

  public void importFromObsidian(MultipartFile file, Notebook notebook) throws IOException {
    try (ZipInputStream zipIn = new ZipInputStream(file.getInputStream())) {
      processZipEntries(zipIn, notebook);
    }
  }

  private void processZipEntries(ZipInputStream zipIn, Notebook notebook) throws IOException {
    ZipEntry entry;
    while ((entry = zipIn.getNextEntry()) != null) {
      if (isHiddenFile(entry.getName())) {
        continue;
      }
      processEntry(entry, notebook, zipIn);
    }
  }

  private boolean isHiddenFile(String entryName) {
    return entryName.startsWith(".") || entryName.contains("/.");
  }

  private void processEntry(ZipEntry entry, Notebook notebook, ZipInputStream zipIn)
      throws IOException {
    if (!entry.getName().endsWith(".md")) {
      return;
    }

    Note currentParent = notebook.getHeadNote();
    String[] pathParts = entry.getName().split("/");

    boolean isIndexFile = pathParts[pathParts.length - 1].equals("__index.md");
    int lastPartIndex = isIndexFile ? pathParts.length - 2 : pathParts.length - 1;

    for (int i = 0; i < lastPartIndex; i++) {
      String part = pathParts[i];
      if (shouldSkipPart(part)) {
        continue;
      }
      currentParent = processNotePart(currentParent, part, entry, zipIn, false);
    }

    if (!isIndexFile) {
      String lastPart = pathParts[lastPartIndex];
      String noteName = removeMarkdownExtension(lastPart);
      processNotePart(currentParent, noteName, entry, zipIn, true);
    }
  }

  private boolean shouldSkipPart(String part) {
    return part.isEmpty() || part.equals(".md");
  }

  private String removeMarkdownExtension(String fileName) {
    return fileName.endsWith(".md") ? fileName.substring(0, fileName.length() - 3) : fileName;
  }

  private Note processNotePart(
      Note currentParent, String noteName, ZipEntry entry, ZipInputStream zipIn, boolean isLastPart)
      throws IOException {
    Note existingNote = findExistingNote(currentParent, noteName);

    if (existingNote != null) {
      return existingNote;
    }

    Note newNote = noteConstructionService.createNote(currentParent, noteName);

    if (!entry.isDirectory() && isLastPart) {
      addContentToNote(newNote, zipIn);
    }

    return newNote;
  }

  private Note findExistingNote(Note parent, String noteName) {
    return parent.getChildren().stream()
        .filter(note -> note.getTopicConstructor().equals(noteName))
        .findFirst()
        .orElse(null);
  }

  private void addContentToNote(Note note, ZipInputStream zipIn) throws IOException {
    String content = new String(zipIn.readAllBytes());

    String[] parts = content.split("---", 3);
    if (parts.length == 3) {
      String frontmatter = parts[1].trim();

      String markdownContent = parts[2].trim();
      if (markdownContent.startsWith("# ")) {
        int nextLineIndex = markdownContent.indexOf('\n');
        if (nextLineIndex != -1) {
          markdownContent = markdownContent.substring(nextLineIndex).trim();
        }
      }
      note.prependDescription(markdownContent);
    } else {
      note.prependDescription(content);
    }
    modelFactoryService.save(note);
  }
}
