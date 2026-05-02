package com.odde.doughnut.services;

import com.odde.doughnut.algorithms.NoteDetailsMarkdown;
import com.odde.doughnut.entities.Folder;
import com.odde.doughnut.entities.Note;
import com.odde.doughnut.entities.Notebook;
import com.odde.doughnut.entities.repositories.NoteRepository;
import com.odde.doughnut.factoryServices.EntityPersister;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

@Service
public class ObsidianFormatService {

  private final NoteRepository noteRepository;
  private final EntityPersister entityPersister;
  private final NoteConstructionService noteConstructionService;
  private final NotebookService notebookService;
  private final Set<String> usedPaths = new HashSet<>();

  public ObsidianFormatService(
      NoteRepository noteRepository,
      EntityPersister entityPersister,
      NoteConstructionService noteConstructionService,
      NotebookService notebookService) {
    this.noteRepository = noteRepository;
    this.entityPersister = entityPersister;
    this.noteConstructionService = noteConstructionService;
    this.notebookService = notebookService;
  }

  public byte[] exportToObsidian(Notebook notebook) throws IOException {
    try (var baos = new ByteArrayOutputStream();
        var zos = new ZipOutputStream(baos)) {

      usedPaths.clear();

      Optional<Note> indexNote = notebookService.findOptionalIndexNote(notebook);
      List<Note> notesInNotebook =
          noteRepository.findAllNonDeletedNotesByNotebookIdOrderByIdAsc(notebook.getId());

      if (indexNote.isPresent()) {
        writeIndexMarkdownAtZipRoot(indexNote.get(), zos);
      }

      Integer indexId = indexNote.map(Note::getId).orElse(null);
      for (Note note : notesInNotebook) {
        if (Objects.equals(note.getId(), indexId)) {
          continue;
        }
        emitObsidianNoteZipEntry(note, notesInNotebook, indexNote, zos);
      }

      zos.close();
      return baos.toByteArray();
    }
  }

  private void writeIndexMarkdownAtZipRoot(Note indexNote, ZipOutputStream zos) throws IOException {
    String filePath = "index.md";
    if (usedPaths.contains(filePath)) {
      return;
    }
    usedPaths.add(filePath);
    String fileContent = generateMarkdownContent(indexNote);
    zos.putNextEntry(new ZipEntry(filePath));
    zos.write(fileContent.getBytes());
  }

  private void emitObsidianNoteZipEntry(
      Note note, List<Note> notesInNotebook, Optional<Note> indexNote, ZipOutputStream zos)
      throws IOException {
    String filePath = zipRelativePath(note, notesInNotebook, indexNote);

    if (usedPaths.contains(filePath)) {
      return;
    }
    usedPaths.add(filePath);

    String fileContent = generateMarkdownContent(note);
    zos.putNextEntry(new ZipEntry(filePath));
    zos.write(fileContent.getBytes());
  }

  private List<String> folderSegmentsFromNotebookRoot(Folder deepest) {
    ArrayDeque<String> segments = new ArrayDeque<>();
    for (Folder folder = deepest; folder != null; folder = folder.getParentFolder()) {
      segments.addFirst(folder.getName());
    }
    return new ArrayList<>(segments);
  }

  private List<String> ancestorFolderLikeSegmentsFromParentPointers(Note note) {
    ArrayDeque<String> segments = new ArrayDeque<>();
    for (Note parent = note.getParent(); parent != null; parent = parent.getParent()) {
      segments.addFirst(parent.getTitle());
    }
    return new ArrayList<>(segments);
  }

  private List<String> stripLeadingIndexSegment(
      List<String> segmentsBeforeTitleSanitize, Optional<Note> indexNote) {
    if (segmentsBeforeTitleSanitize.isEmpty() || indexNote.isEmpty()) {
      return segmentsBeforeTitleSanitize;
    }
    String indexTitleForMatch = indexNote.get().getTitle();
    if (!segmentsBeforeTitleSanitize.getFirst().equalsIgnoreCase(indexTitleForMatch)) {
      return segmentsBeforeTitleSanitize;
    }
    return new ArrayList<>(
        segmentsBeforeTitleSanitize.subList(1, segmentsBeforeTitleSanitize.size()));
  }

  private List<String> obsidianDirSegmentsBeforeTitle(Note note, Optional<Note> indexNote) {
    List<String> segments =
        note.getFolder() != null
            ? folderSegmentsFromNotebookRoot(note.getFolder())
            : ancestorFolderLikeSegmentsFromParentPointers(note);
    return stripLeadingIndexSegment(segments, indexNote);
  }

  private boolean isOptionalIndexNote(Note note, Optional<Note> indexNote) {
    return indexNote.filter(i -> Objects.equals(note.getId(), i.getId())).isPresent();
  }

  private boolean noteExportsAsObsidianSubtree(
      Note note, List<Note> notesInNotebook, Optional<Note> indexNote) {
    if (!note.getChildren().isEmpty()) {
      return true;
    }
    List<String> prefixBelowTitle = obsidianDirSegmentsBeforeTitle(note, indexNote);
    ArrayList<String> childScope = new ArrayList<>(prefixBelowTitle);
    childScope.add(note.getTitle());

    return notesInNotebook.stream()
        .filter(m -> !isOptionalIndexNote(m, indexNote))
        .filter(m -> !Objects.equals(m.getId(), note.getId()))
        .anyMatch(m -> obsidianDirSegmentsBeforeTitle(m, indexNote).equals(childScope));
  }

  private String zipRelativePath(Note note, List<Note> notesInNotebook, Optional<Note> indexNote) {
    List<String> dirSegmentsSanitized =
        obsidianDirSegmentsBeforeTitle(note, indexNote).stream()
            .map(this::sanitizeFileName)
            .toList();
    String sanitizedTitle = sanitizeFileName(note.getTitle());
    boolean hasSubtree = noteExportsAsObsidianSubtree(note, notesInNotebook, indexNote);

    String tail = hasSubtree ? sanitizedTitle + "/__index.md" : sanitizedTitle + ".md";

    return dirSegmentsSanitized.isEmpty()
        ? tail
        : String.join("/", dirSegmentsSanitized) + "/" + tail;
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
    String title = note.getTitle() != null ? note.getTitle() : "";
    return """
           # %s
           %s"""
        .formatted(title, note.getDetails());
  }

  private String sanitizeFileName(String fileName) {
    if (fileName == null) {
      return "untitled";
    }
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

    Note currentParent = notebookService.findOptionalIndexNote(notebook).orElse(null);
    String[] pathParts = entry.getName().split("/");

    boolean isIndexFile = pathParts[pathParts.length - 1].equals("__index.md");
    int lastPartIndex = isIndexFile ? pathParts.length - 2 : pathParts.length - 1;

    for (int i = 1; i < lastPartIndex; i++) {
      String part = pathParts[i];
      if (shouldSkipPart(part)) {
        continue;
      }
      currentParent = processNotePart(notebook, currentParent, part, entry, zipIn, false);
    }

    if (!isIndexFile) {
      String lastPart = pathParts[lastPartIndex];
      String noteName = removeMarkdownExtension(lastPart);
      processNotePart(notebook, currentParent, noteName, entry, zipIn, true);
    }
  }

  private boolean shouldSkipPart(String part) {
    return part.isEmpty() || part.equals(".md");
  }

  private String removeMarkdownExtension(String fileName) {
    return fileName.endsWith(".md") ? fileName.substring(0, fileName.length() - 3) : fileName;
  }

  private Note processNotePart(
      Notebook notebook,
      Note currentParent,
      String noteName,
      ZipEntry entry,
      ZipInputStream zipIn,
      boolean isLastPart)
      throws IOException {
    if (currentParent != null) {
      currentParent = noteRepository.findById(currentParent.getId()).orElse(null);
    }
    Note existingNote = findExistingNote(notebook, currentParent, noteName);

    if (existingNote != null) {
      return existingNote;
    }

    Note newNote = noteConstructionService.createNote(notebook, currentParent, noteName);
    newNote = entityPersister.save(newNote);

    // Force a flush to ensure the relationship is persisted
    entityPersister.flush();

    // Refresh both entities to get the latest state
    entityPersister.refresh(newNote);
    if (currentParent != null) {
      entityPersister.refresh(currentParent);
    }

    if (!entry.isDirectory() && isLastPart) {
      addContentToNote(newNote, zipIn);
    }
    return newNote;
  }

  private Note findExistingNote(Notebook notebook, Note parent, String noteName) {
    if (parent != null) {
      return parent.getChildren().stream()
          .filter(note -> Objects.equals(note.getTitle(), noteName))
          .findFirst()
          .orElse(null);
    }
    return noteRepository.findNotebookRootNotesByNotebookId(notebook.getId()).stream()
        .filter(note -> Objects.equals(note.getTitle(), noteName))
        .findFirst()
        .orElse(null);
  }

  private void addContentToNote(Note note, ZipInputStream zipIn) throws IOException {
    String content = new String(zipIn.readAllBytes());

    NoteDetailsMarkdown.LeadingFrontmatter frontmatter =
        NoteDetailsMarkdown.splitLeadingFrontmatter(content).orElse(null);
    if (frontmatter != null) {
      Map<String, String> metadata = parseFrontmatter(frontmatter.yamlRaw().trim());

      // Check if note with this ID exists
      if (metadata.containsKey("note_id")) {
        Integer noteId = Integer.parseInt(metadata.get("note_id"));
        Note existingNote = noteRepository.findById(noteId).orElse(null);

        if (existingNote != null) {
          // Update existing note instead of creating new one
          updateExistingNote(existingNote, frontmatter.body().trim(), note.getTitle());
          // Copy the children to the existing note
          note.getChildren().forEach(child -> child.setParentNote(existingNote));
          // Remove the temporary note
          entityPersister.remove(note);
          return;
        }
      }

      // Process content for new note
      String markdownContent = frontmatter.body().trim();
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
    entityPersister.save(note);
  }

  private Map<String, String> parseFrontmatter(String frontmatter) {
    Map<String, String> metadata = new HashMap<>();
    String[] lines = frontmatter.split("\n");
    for (String line : lines) {
      String[] parts = line.split(":", 2);
      if (parts.length == 2) {
        metadata.put(parts[0].trim(), parts[1].trim());
      }
    }
    return metadata;
  }

  private void updateExistingNote(Note existingNote, String content, String newTitle) {
    existingNote.setTitle(newTitle);
    if (content.startsWith("# ")) {
      int nextLineIndex = content.indexOf('\n');
      if (nextLineIndex != -1) {
        content = content.substring(nextLineIndex).trim();
      }
    }
    existingNote.prependDescription(content);
    entityPersister.save(existingNote);
  }
}
