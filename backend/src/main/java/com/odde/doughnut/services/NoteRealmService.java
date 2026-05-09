package com.odde.doughnut.services;

import com.odde.doughnut.algorithms.Frontmatter;
import com.odde.doughnut.algorithms.NoteContentMarkdown;
import com.odde.doughnut.controllers.dto.FolderTrailSegments;
import com.odde.doughnut.controllers.dto.NoteRealm;
import com.odde.doughnut.entities.Folder;
import com.odde.doughnut.entities.Note;
import com.odde.doughnut.entities.User;
import com.odde.doughnut.entities.repositories.NoteRepository;
import com.odde.doughnut.services.index.IndexScope;
import com.odde.doughnut.services.index.ScopedIndexNoteService;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.springframework.stereotype.Service;

@Service
public class NoteRealmService {

  /** Canonical `title_pattern` first; legacy camelCase supported for existing notes. */
  private static final List<String> TITLE_PATTERN_KEYS = List.of("title_pattern", "titlePattern");

  private final WikiTitleCacheService wikiTitleCacheService;
  private final NoteRepository noteRepository;
  private final NotebookCatalogService notebookCatalogService;
  private final ScopedIndexNoteService scopedIndexNoteService;

  public NoteRealmService(
      WikiTitleCacheService wikiTitleCacheService,
      NoteRepository noteRepository,
      NotebookCatalogService notebookCatalogService,
      ScopedIndexNoteService scopedIndexNoteService) {
    this.wikiTitleCacheService = wikiTitleCacheService;
    this.noteRepository = noteRepository;
    this.notebookCatalogService = notebookCatalogService;
    this.scopedIndexNoteService = scopedIndexNoteService;
  }

  public NoteRealm build(Note note, User viewer) {
    Note focus = hydrateNote(note);
    var wikiTitles = wikiTitleCacheService.wikiTitlesForViewer(focus, viewer);
    NoteRealm realm = new NoteRealm(focus, wikiTitles);
    List<Note> refNotes =
        hydrateNoteList(wikiTitleCacheService.referencesNotesForViewer(focus, viewer));
    realm.setReferences(refNotes.stream().map(Note::getNoteTopology).toList());
    realm.setNotebookView(notebookCatalogService.clientViewFor(focus.getNotebook(), viewer));
    realm.setAncestorFolders(FolderTrailSegments.fromRootToContainingFolder(focus));
    realm.setIndexNoteContent(resolveIndexNoteContentForScopedTitlePattern(focus));
    return realm;
  }

  private String resolveIndexNoteContentForScopedTitlePattern(Note focus) {
    if (focus.getNotebook() == null) {
      return null;
    }
    List<Folder> outerToInner = FolderTrailSegments.fromRootToContainingFolder(focus);
    for (int i = outerToInner.size() - 1; i >= 0; i--) {
      Optional<Note> designated =
          scopedIndexNoteService.findDesignatedIndexNote(
              new IndexScope.FolderIndex(outerToInner.get(i)));
      if (designated.isPresent() && hasNonBlankTitlePattern(designated.get())) {
        return designated.get().getContent();
      }
    }
    return scopedIndexNoteService
        .findDesignatedIndexNote(new IndexScope.NotebookRoot(focus.getNotebook()))
        .filter(this::hasNonBlankTitlePattern)
        .map(Note::getContent)
        .orElse(null);
  }

  private boolean hasNonBlankTitlePattern(Note indexNote) {
    String content = indexNote.getContent();
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
