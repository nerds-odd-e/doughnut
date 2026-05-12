package com.odde.doughnut.services;

import com.odde.doughnut.algorithms.Frontmatter;
import com.odde.doughnut.algorithms.NoteContentMarkdown;
import com.odde.doughnut.controllers.dto.FolderTrailSegments;
import com.odde.doughnut.controllers.dto.NoteRealm;
import com.odde.doughnut.entities.Folder;
import com.odde.doughnut.entities.Note;
import com.odde.doughnut.entities.Notebook;
import com.odde.doughnut.entities.User;
import com.odde.doughnut.entities.repositories.NoteRepository;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.springframework.stereotype.Service;

@Service
public class NoteRealmService {

  /** Canonical `title_pattern` first; legacy camelCase supported for existing notes. */
  private static final List<String> TITLE_PATTERN_KEYS = List.of("title_pattern", "titlePattern");

  /** Canonical key first; legacy camelCase supported for existing notes. */
  private static final List<String> QUESTION_GENERATION_INSTRUCTION_KEYS =
      List.of("question_generation_instruction", "questionGenerationInstruction");

  private final WikiTitleCacheService wikiTitleCacheService;
  private final NoteRepository noteRepository;
  private final NotebookCatalogService notebookCatalogService;

  public NoteRealmService(
      WikiTitleCacheService wikiTitleCacheService,
      NoteRepository noteRepository,
      NotebookCatalogService notebookCatalogService) {
    this.wikiTitleCacheService = wikiTitleCacheService;
    this.noteRepository = noteRepository;
    this.notebookCatalogService = notebookCatalogService;
  }

  public NoteRealm build(Note note, User viewer) {
    Note focus = hydrateNote(note);
    var wikiTitles = wikiTitleCacheService.wikiTitlesForViewer(focus, viewer);
    NoteRealm realm = new NoteRealm(focus, wikiTitles);
    List<Note> refNotes =
        hydrateNoteList(wikiTitleCacheService.referencesNotesForViewer(focus, viewer));
    realm.setReferences(refNotes.stream().map(Note::getNoteTopology).toList());
    realm.setNotebookRealm(notebookCatalogService.notebookRealmFor(focus.getNotebook(), viewer));
    realm.setAncestorFolders(FolderTrailSegments.fromRootToContainingFolder(focus));
    realm.setIndexNoteContent(resolveIndexNoteContentForNote(focus));
    return realm;
  }

  public String resolveIndexNoteContentForFolder(Folder folder) {
    if (folder.getNotebook() == null) {
      return null;
    }
    return resolveIndexNoteContent(
        FolderTrailSegments.fromRootToFolder(folder), folder.getNotebook());
  }

  /**
   * Nearest non-blank {@code question_generation_instruction} from container {@code indexContent}:
   * inner folder → parent folders → notebook root.
   */
  public Optional<String> resolveScopedQuestionGenerationInstruction(Note focus) {
    if (focus.getNotebook() == null) {
      return Optional.empty();
    }
    List<Folder> outerToInner = FolderTrailSegments.fromRootToContainingFolder(focus);
    for (int i = outerToInner.size() - 1; i >= 0; i--) {
      Optional<String> instruction =
          questionGenerationInstructionFromContent(outerToInner.get(i).getIndexContent());
      if (instruction.isPresent()) {
        return instruction;
      }
    }
    return questionGenerationInstructionFromContent(focus.getNotebook().getIndexContent());
  }

  private String resolveIndexNoteContentForNote(Note focus) {
    if (focus.getNotebook() == null) {
      return null;
    }
    return resolveIndexNoteContent(
        FolderTrailSegments.fromRootToContainingFolder(focus), focus.getNotebook());
  }

  private String resolveIndexNoteContent(List<Folder> outerToInner, Notebook notebook) {
    for (int i = outerToInner.size() - 1; i >= 0; i--) {
      String content = outerToInner.get(i).getIndexContent();
      if (hasNonBlankTitlePatternInContent(content)) {
        return content;
      }
    }
    String nbContent = notebook.getIndexContent();
    return hasNonBlankTitlePatternInContent(nbContent) ? nbContent : null;
  }

  private boolean hasNonBlankTitlePatternInContent(String content) {
    if (content == null || content.isBlank()) {
      return false;
    }
    return NoteContentMarkdown.splitLeadingFrontmatter(content)
        .map(NoteContentMarkdown.LeadingFrontmatter::frontmatter)
        .filter(this::frontmatterHasNonBlankTitlePattern)
        .isPresent();
  }

  private boolean frontmatterHasNonBlankTitlePattern(Frontmatter fm) {
    for (String key : TITLE_PATTERN_KEYS) {
      if (fm.getString(key).map(String::trim).filter(s -> !s.isEmpty()).isPresent()) {
        return true;
      }
    }
    return false;
  }

  private Optional<String> questionGenerationInstructionFromContent(String content) {
    if (content == null || content.isBlank()) {
      return Optional.empty();
    }
    return NoteContentMarkdown.splitLeadingFrontmatter(content)
        .map(NoteContentMarkdown.LeadingFrontmatter::frontmatter)
        .flatMap(this::questionInstructionFromFrontmatter);
  }

  private Optional<String> questionInstructionFromFrontmatter(Frontmatter fm) {
    for (String key : QUESTION_GENERATION_INSTRUCTION_KEYS) {
      Optional<String> value = fm.getString(key).map(String::trim).filter(s -> !s.isEmpty());
      if (value.isPresent()) {
        return value;
      }
    }
    return Optional.empty();
  }

  /** Re-load notes with associations so JSON serialization does not hit Hibernate proxies. */
  private Note hydrateNote(Note note) {
    return noteRepository
        .hydrateNonDeletedNotesWithNotebookAndFolderByIds(List.of(note.getId()))
        .stream()
        .findFirst()
        .orElse(note);
  }

  private List<Note> hydrateNoteList(List<Note> notes) {
    if (notes.isEmpty()) {
      return notes;
    }
    List<Integer> ids = notes.stream().map(Note::getId).distinct().toList();
    Map<Integer, Note> byId = new LinkedHashMap<>();
    for (Note n : noteRepository.hydrateNonDeletedNotesWithNotebookAndFolderByIds(ids)) {
      byId.putIfAbsent(n.getId(), n);
    }
    return notes.stream().map(n -> byId.getOrDefault(n.getId(), n)).toList();
  }
}
