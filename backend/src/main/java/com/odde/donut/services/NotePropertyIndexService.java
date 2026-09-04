package com.odde.donut.services;

import com.odde.donut.algorithms.AuthoredNoteReference;
import com.odde.donut.algorithms.CanonicalDonutOrigin;
import com.odde.donut.algorithms.NoteContentMarkdown;
import com.odde.donut.algorithms.NotePropertyIndexPlanner;
import com.odde.donut.entities.AuthoredNoteReferenceRow;
import com.odde.donut.entities.Note;
import com.odde.donut.entities.NotePropertyIndex;
import com.odde.donut.entities.repositories.NotePropertyIndexRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.FlushModeType;
import jakarta.persistence.PersistenceContext;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class NotePropertyIndexService {

  @PersistenceContext private EntityManager entityManager;

  private final NotePropertyIndexRepository notePropertyIndexRepository;
  private final CanonicalDonutOrigin canonicalDonutOrigin;

  public NotePropertyIndexService(
      NotePropertyIndexRepository notePropertyIndexRepository,
      CanonicalDonutOrigin canonicalDonutOrigin) {
    this.notePropertyIndexRepository = notePropertyIndexRepository;
    this.canonicalDonutOrigin = canonicalDonutOrigin;
  }

  @Transactional
  public void refreshForNote(Note note) {
    Integer noteId = note.getId();
    FlushModeType previousFlushMode = entityManager.getFlushMode();
    entityManager.setFlushMode(FlushModeType.COMMIT);
    try {
      // replaceContent may leave new authored-reference children transient until flush; break FK
      // links on existing index rows first so flush can persist the note aggregate safely.
      for (NotePropertyIndex existingRow :
          notePropertyIndexRepository.findByNote_IdOrderByIdAsc(noteId)) {
        existingRow.setAuthoredNoteReference(null);
        entityManager.remove(existingRow);
      }
      entityManager.flush();
    } finally {
      entityManager.setFlushMode(previousFlushMode);
    }
    Note indexOwner = entityManager.getReference(Note.class, noteId);
    NoteContentMarkdown.splitLeadingFrontmatter(note.getContent() == null ? "" : note.getContent())
        .ifPresent(
            lf -> {
              Map<String, AuthoredNoteReferenceRow> bySourceLocalKey =
                  ownRowsBySourceLocalKey(noteId);
              Map<String, List<NotePropertyIndexPlanner.PlannedRow>> rowsByKey =
                  new LinkedHashMap<>();
              for (NotePropertyIndexPlanner.PlannedRow planned :
                  NotePropertyIndexPlanner.plannedRows(lf.frontmatter(), canonicalDonutOrigin)) {
                rowsByKey
                    .computeIfAbsent(planned.propertyKey(), k -> new ArrayList<>())
                    .add(planned);
              }
              rowsByKey.forEach(
                  (propertyKey, plannedRows) ->
                      persistRowsForPropertyKey(
                          indexOwner, propertyKey, plannedRows, bySourceLocalKey));
            });
  }

  public List<AuthoredNoteReference> authoredReferencesForProperty(
      Note note, String propertyKey, List<NotePropertyIndex> indexRows) {
    List<String> plannedSourceLocalKeys = plannedSourceLocalKeys(note, propertyKey);
    if (plannedSourceLocalKeys.isEmpty()) {
      return List.of();
    }

    Map<String, AuthoredNoteReference> indexedReferences = new HashMap<>();
    for (NotePropertyIndex indexRow : indexRows) {
      if (indexRow.getAuthoredNoteReference() != null) {
        AuthoredNoteReference reference = indexRow.getAuthoredNoteReference().toDomainReference();
        indexedReferences.putIfAbsent(reference.sourceLocalKey(), reference);
      }
    }
    if (indexedReferences.keySet().containsAll(plannedSourceLocalKeys)) {
      return plannedSourceLocalKeys.stream().map(indexedReferences::get).toList();
    }

    Map<String, AuthoredNoteReferenceRow> authoredRows = ownRowsBySourceLocalKey(note.getId());
    return plannedSourceLocalKeys.stream()
        .map(authoredRows::get)
        .filter(Objects::nonNull)
        .map(AuthoredNoteReferenceRow::toDomainReference)
        .toList();
  }

  private List<String> plannedSourceLocalKeys(Note note, String propertyKey) {
    return NoteContentMarkdown.splitLeadingFrontmatter(
            note.getContent() == null ? "" : note.getContent())
        .map(
            leadingFrontmatter ->
                NotePropertyIndexPlanner.plannedRows(
                        leadingFrontmatter.frontmatter(), canonicalDonutOrigin)
                    .stream()
                    .filter(planned -> planned.propertyKey().equals(propertyKey))
                    .map(NotePropertyIndexPlanner.PlannedRow::sourceLocalKey)
                    .filter(Objects::nonNull)
                    .distinct()
                    .toList())
        .orElseGet(List::of);
  }

  private Map<String, AuthoredNoteReferenceRow> ownRowsBySourceLocalKey(Integer noteId) {
    List<AuthoredNoteReferenceRow> ownRows =
        entityManager
            .createQuery(
                "FROM AuthoredNoteReferenceRow r WHERE r.note.id = :noteId",
                AuthoredNoteReferenceRow.class)
            .setParameter("noteId", noteId)
            .getResultList();
    Map<String, AuthoredNoteReferenceRow> bySourceLocalKey = new HashMap<>();
    for (AuthoredNoteReferenceRow row : ownRows) {
      bySourceLocalKey.putIfAbsent(row.toDomainReference().sourceLocalKey(), row);
    }
    return bySourceLocalKey;
  }

  private void persistRowsForPropertyKey(
      Note indexOwner,
      String propertyKey,
      List<NotePropertyIndexPlanner.PlannedRow> plannedRows,
      Map<String, AuthoredNoteReferenceRow> bySourceLocalKey) {
    if (plannedRows.size() == 1 && !plannedRows.getFirst().listProperty()) {
      saveIndexRow(
          indexOwner,
          propertyKey,
          plannedRows.getFirst().itemIndex(),
          resolveAuthoredReference(plannedRows.getFirst(), bySourceLocalKey));
      return;
    }

    List<IndexedListReference> indexedReferences = new ArrayList<>();
    for (NotePropertyIndexPlanner.PlannedRow planned : plannedRows) {
      if (planned.valueText() == null || planned.valueText().isBlank()) {
        continue;
      }
      resolveAuthoredReference(planned, bySourceLocalKey)
          .ifPresent(
              reference ->
                  indexedReferences.add(new IndexedListReference(planned.itemIndex(), reference)));
    }

    if (indexedReferences.isEmpty()) {
      saveIndexRow(indexOwner, propertyKey, 0, Optional.empty());
      return;
    }

    for (IndexedListReference indexed : indexedReferences) {
      saveIndexRow(
          indexOwner, propertyKey, indexed.itemIndex(), Optional.of(indexed.authoredReference()));
    }
  }

  private void saveIndexRow(
      Note indexOwner,
      String propertyKey,
      int itemIndex,
      Optional<AuthoredNoteReferenceRow> authoredReference) {
    NotePropertyIndex row = new NotePropertyIndex();
    row.setNote(indexOwner);
    row.setPropertyKey(propertyKey);
    row.setItemIndex(itemIndex);
    authoredReference.ifPresent(row::setAuthoredNoteReference);
    notePropertyIndexRepository.save(row);
  }

  private Optional<AuthoredNoteReferenceRow> resolveAuthoredReference(
      NotePropertyIndexPlanner.PlannedRow planned,
      Map<String, AuthoredNoteReferenceRow> bySourceLocalKey) {
    return Optional.ofNullable(planned.sourceLocalKey()).map(bySourceLocalKey::get);
  }

  private record IndexedListReference(int itemIndex, AuthoredNoteReferenceRow authoredReference) {}
}
