package com.odde.donut.services.notebookGit;

import com.odde.donut.entities.NotePropertyIndex;
import com.odde.donut.entities.NotebookGitBinding;
import com.odde.donut.entities.repositories.NotePropertyIndexRepository;
import com.odde.donut.entities.repositories.NotebookGitBindingRepository;
import com.odde.donut.services.NotePropertyIndexService;
import com.odde.donut.services.notebookGit.NotebookGitAcceptedRepositoryStore.OpenedAcceptedRepository;
import com.odde.donut.services.notebookGit.NotebookGitPropertyValueDating.ValueDate;
import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import org.springframework.data.domain.Limit;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Fills the change dates a notebook's property values are missing from its retained accepted
 * history; a value the history does not date (present at its root) takes its note's creation time,
 * so a rebuilt notebook has no undated value left. Dated values are authoritative and kept.
 */
@Service
public class NotePropertyValueDateRebuildService {

  private record ValueAtPath(String path, String propertyKey, String propertyValue) {
    static ValueAtPath of(ValueDate date) {
      return new ValueAtPath(date.tipPath(), date.propertyKey(), date.propertyValue());
    }

    static ValueAtPath of(NotePropertyIndex row) {
      return new ValueAtPath(
          NotebookGitPortablePath.ofNote(row.getNote()),
          row.getPropertyKey(),
          row.getPropertyValue());
    }
  }

  private final NotePropertyIndexRepository notePropertyIndexRepository;
  private final NotebookGitBindingRepository bindingRepository;
  private final NotebookGitAcceptedRepositoryStore repositoryStore;
  private final NotePropertyIndexService notePropertyIndexService;

  NotePropertyValueDateRebuildService(
      NotePropertyIndexRepository notePropertyIndexRepository,
      NotebookGitBindingRepository bindingRepository,
      NotebookGitAcceptedRepositoryStore repositoryStore,
      NotePropertyIndexService notePropertyIndexService) {
    this.notePropertyIndexRepository = notePropertyIndexRepository;
    this.bindingRepository = bindingRepository;
    this.repositoryStore = repositoryStore;
    this.notePropertyIndexService = notePropertyIndexService;
  }

  /** Dates one notebook's undated values; false when no notebook has any. */
  @Transactional
  public boolean rebuildOne() {
    return notePropertyIndexRepository.findNotebookIdsWithUndatedValues(Limit.of(1)).stream()
        .findFirst()
        .map(notebookId -> {
          rebuild(notebookId);
          return true;
        })
        .orElse(false);
  }

  private void rebuild(Integer notebookId) {
    NotebookGitBinding binding =
        bindingRepository.findByNotebookIdForUpdate(notebookId).orElseThrow();
    Map<ValueAtPath, Optional<Instant>> datesByValue;
    try (OpenedAcceptedRepository accepted = repositoryStore.open(binding)) {
      datesByValue =
          NotebookGitPropertyValueDating.dateValues(accepted.repository(), null, accepted.head())
              .stream()
              .collect(Collectors.toMap(ValueAtPath::of, ValueDate::committedAt));
    }
    notePropertyIndexRepository
        .findByNote_Notebook_IdAndChangedAtIsNull(notebookId)
        .forEach(row ->
            notePropertyIndexService.writeValueDate(
                row.getNote(),
                row.getPropertyKey(),
                row.getPropertyValue(),
                datesByValue.getOrDefault(ValueAtPath.of(row), Optional.empty())));
  }
}
