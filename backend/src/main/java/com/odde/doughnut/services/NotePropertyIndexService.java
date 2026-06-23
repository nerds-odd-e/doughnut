package com.odde.doughnut.services;

import com.odde.doughnut.algorithms.NoteContentMarkdown;
import com.odde.doughnut.algorithms.NotePropertyIndexPlanner;
import com.odde.doughnut.algorithms.WikiLinkMarkdown;
import com.odde.doughnut.entities.Note;
import com.odde.doughnut.entities.NotePropertyIndex;
import com.odde.doughnut.entities.repositories.NotePropertyIndexRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.LockModeType;
import jakarta.persistence.PersistenceContext;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class NotePropertyIndexService {

  @PersistenceContext private EntityManager entityManager;

  private final NotePropertyIndexRepository notePropertyIndexRepository;
  private final WikiLinkResolver wikiLinkResolver;

  public NotePropertyIndexService(
      NotePropertyIndexRepository notePropertyIndexRepository, WikiLinkResolver wikiLinkResolver) {
    this.notePropertyIndexRepository = notePropertyIndexRepository;
    this.wikiLinkResolver = wikiLinkResolver;
  }

  @Transactional
  public void refreshForNote(Note note) {
    Integer noteId = note.getId();
    entityManager.find(Note.class, noteId, LockModeType.PESSIMISTIC_WRITE);
    notePropertyIndexRepository.deleteByNoteIdInBulk(noteId);
    entityManager.flush();
    Note indexOwner = entityManager.getReference(Note.class, noteId);
    NoteContentMarkdown.splitLeadingFrontmatter(note.getContent() == null ? "" : note.getContent())
        .ifPresent(
            lf -> {
              Map<String, List<NotePropertyIndexPlanner.PlannedRow>> rowsByKey =
                  new LinkedHashMap<>();
              for (NotePropertyIndexPlanner.PlannedRow planned :
                  NotePropertyIndexPlanner.plannedRows(lf.frontmatter())) {
                rowsByKey
                    .computeIfAbsent(planned.propertyKey(), k -> new ArrayList<>())
                    .add(planned);
              }
              rowsByKey.forEach(
                  (propertyKey, plannedRows) ->
                      persistRowsForPropertyKey(indexOwner, propertyKey, plannedRows));
            });
  }

  private void persistRowsForPropertyKey(
      Note indexOwner, String propertyKey, List<NotePropertyIndexPlanner.PlannedRow> plannedRows) {
    if (plannedRows.size() == 1 && !plannedRows.getFirst().listProperty()) {
      saveIndexRow(
          indexOwner,
          propertyKey,
          plannedRows.getFirst().itemIndex(),
          resolveTargetNoteFromPropertyValue(plannedRows.getFirst().valueText(), indexOwner));
      return;
    }

    List<ResolvedListTarget> resolvedTargets = new ArrayList<>();
    for (NotePropertyIndexPlanner.PlannedRow planned : plannedRows) {
      if (planned.valueText() == null || planned.valueText().isBlank()) {
        continue;
      }
      resolveTargetNoteFromPropertyValue(planned.valueText(), indexOwner)
          .ifPresent(
              target -> resolvedTargets.add(new ResolvedListTarget(planned.itemIndex(), target)));
    }

    if (resolvedTargets.isEmpty()) {
      saveIndexRow(indexOwner, propertyKey, 0, Optional.empty());
      return;
    }

    for (ResolvedListTarget resolved : resolvedTargets) {
      saveIndexRow(indexOwner, propertyKey, resolved.itemIndex(), Optional.of(resolved.target()));
    }
  }

  private void saveIndexRow(
      Note indexOwner, String propertyKey, int itemIndex, Optional<Note> targetNote) {
    NotePropertyIndex row = new NotePropertyIndex();
    row.setNote(indexOwner);
    row.setPropertyKey(propertyKey);
    row.setItemIndex(itemIndex);
    targetNote.ifPresent(row::setTargetNote);
    notePropertyIndexRepository.save(row);
  }

  private Optional<Note> resolveTargetNoteFromPropertyValue(String value, Note focusNote) {
    if (value == null || value.isBlank()) {
      return Optional.empty();
    }
    List<String> linkTokens = WikiLinkMarkdown.innerTitlesInOccurrenceOrder(value);
    if (linkTokens.isEmpty()) {
      return Optional.empty();
    }
    return wikiLinkResolver.resolveAnyTargetWikiLinkToken(linkTokens.getFirst(), focusNote);
  }

  private record ResolvedListTarget(int itemIndex, Note target) {}
}
