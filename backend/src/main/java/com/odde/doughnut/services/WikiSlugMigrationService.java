package com.odde.doughnut.services;

import com.odde.doughnut.controllers.dto.WikiSlugMigrationBatchResult;
import com.odde.doughnut.controllers.dto.WikiSlugMigrationStatus;
import com.odde.doughnut.entities.Folder;
import com.odde.doughnut.entities.Note;
import com.odde.doughnut.entities.repositories.FolderRepository;
import com.odde.doughnut.entities.repositories.NoteRepository;
import com.odde.doughnut.utils.WikiSlugGeneration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class WikiSlugMigrationService {

  static final int DEFAULT_BATCH_LIMIT = 100;
  static final int MAX_BATCH_LIMIT = 500;

  private final FolderRepository folderRepository;
  private final NoteRepository noteRepository;

  public WikiSlugMigrationService(
      FolderRepository folderRepository, NoteRepository noteRepository) {
    this.folderRepository = folderRepository;
    this.noteRepository = noteRepository;
  }

  @Transactional
  public WikiSlugMigrationBatchResult migrateFoldersBatch(int limit) {
    int capped = capLimit(limit);
    List<Folder> batch =
        folderRepository.findFoldersReadyForSlugMigration(PageRequest.of(0, capped));
    Map<NotebookParentKey, Set<String>> reservedBasenames = new HashMap<>();
    for (Folder folder : batch) {
      Integer notebookId = folder.getNotebook().getId();
      Integer parentFolderId =
          folder.getParentFolder() == null ? null : folder.getParentFolder().getId();
      NotebookParentKey key = new NotebookParentKey(notebookId, parentFolderId);
      List<String> siblingSlugs =
          folderRepository.findSlugsOfSiblingFolders(notebookId, parentFolderId);
      Set<String> taken = basenamesFromSlugs(siblingSlugs);
      taken.addAll(reservedBasenames.getOrDefault(key, Set.of()));
      String basename = WikiSlugGeneration.uniqueSlugWithin(folder.getName(), taken);
      reservedBasenames.computeIfAbsent(key, k -> new HashSet<>()).add(basename);
      Folder parent = folder.getParentFolder();
      if (parent == null) {
        folder.setSlug(basename);
      } else {
        folder.setSlug(parent.getSlug() + "/" + basename);
      }
      folderRepository.save(folder);
    }
    return resultWithStatus(batch.size());
  }

  @Transactional
  public WikiSlugMigrationBatchResult migrateNotesBatch(int limit) {
    int capped = capLimit(limit);
    List<Note> batch = noteRepository.findNotesReadyForSlugMigration(PageRequest.of(0, capped));
    Map<NotebookFolderKey, Set<String>> reservedBasenames = new HashMap<>();
    for (Note note : batch) {
      Integer notebookId = note.getNotebook().getId();
      Folder folder = note.getFolder();
      Integer folderId = folder == null ? null : folder.getId();
      NotebookFolderKey key = new NotebookFolderKey(notebookId, folderId);
      List<String> siblingSlugs =
          noteRepository.findSlugsOfNotesInFolderScope(notebookId, folderId, note.getId());
      Set<String> taken = basenamesFromSlugs(siblingSlugs);
      taken.addAll(reservedBasenames.getOrDefault(key, Set.of()));
      String titleSource = Objects.toString(note.getTitle(), "");
      String basename = WikiSlugGeneration.uniqueSlugWithin(titleSource, taken);
      reservedBasenames.computeIfAbsent(key, k -> new HashSet<>()).add(basename);
      if (folder == null) {
        note.setSlug(basename);
      } else {
        note.setSlug(folder.getSlug() + "/" + basename);
      }
      noteRepository.save(note);
    }
    return resultWithStatus(batch.size());
  }

  private WikiSlugMigrationBatchResult resultWithStatus(long processedInBatch) {
    WikiSlugMigrationBatchResult out = new WikiSlugMigrationBatchResult();
    out.setProcessedInBatch(processedInBatch);
    WikiSlugMigrationStatus status = new WikiSlugMigrationStatus();
    status.setFoldersMissingSlug(folderRepository.countFoldersMissingSlug());
    status.setNotesMissingSlug(noteRepository.countNotesMissingSlug());
    out.setStatus(status);
    return out;
  }

  static int capLimit(int limit) {
    if (limit < 1) {
      return DEFAULT_BATCH_LIMIT;
    }
    return Math.min(limit, MAX_BATCH_LIMIT);
  }

  private static Set<String> basenamesFromSlugs(List<String> slugs) {
    Set<String> set = new HashSet<>();
    for (String s : slugs) {
      if (s == null || s.isEmpty()) {
        continue;
      }
      set.add(WikiSlugPathService.basenameOf(s));
    }
    return set;
  }

  private record NotebookParentKey(Integer notebookId, Integer parentFolderId) {}

  private record NotebookFolderKey(Integer notebookId, Integer folderId) {}
}
