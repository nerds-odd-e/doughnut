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
    return "---\n"
        + "note_id: "
        + note.getId()
        + "\n"
        + "created_at: "
        + note.getCreatedAt()
        + "\n"
        + "updated_at: "
        + note.getUpdatedAt()
        + "\n"
        + "---\n"
        + "# "
        + note.getTopicConstructor()
        + "\n"
        + note.getDetails();
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

  private void processEntry(ZipEntry entry, Notebook notebook, ZipInputStream zipIn) throws IOException {
    Note currentParent = notebook.getHeadNote();
    String[] pathParts = entry.getName().split("/");

    for (int i = 1; i < pathParts.length; i++) {
        String part = pathParts[i];
        
        if (shouldSkipPart(part)) {
            continue;
        }

        String noteName = removeMarkdownExtension(part);
        currentParent = processNotePart(currentParent, noteName, entry, zipIn, i == pathParts.length - 1);
    }
  }

  private boolean shouldSkipPart(String part) {
    return part.isEmpty() || part.equals(".md");
  }

  private String removeMarkdownExtension(String fileName) {
    return fileName.endsWith(".md") 
        ? fileName.substring(0, fileName.length() - 3) 
        : fileName;
  }

  private Note processNotePart(
    Note currentParent, 
    String noteName, 
    ZipEntry entry, 
    ZipInputStream zipIn,
    boolean isLastPart
  ) throws IOException {
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
        .filter(note -> note.getNoteTitle().matches(noteName))
        .findFirst()
        .orElse(null);
  }

  private void addContentToNote(Note note, ZipInputStream zipIn) throws IOException {
    String content = new String(zipIn.readAllBytes());
    note.prependDescription(content);
    modelFactoryService.save(note);
  }
}
