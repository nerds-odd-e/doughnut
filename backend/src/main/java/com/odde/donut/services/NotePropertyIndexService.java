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
import java.util.HashMap;
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
    // Detach any rows already managed in this transaction before the bulk delete, which
    // acts directly on the database and would otherwise leave stale managed copies behind.
    entityManager
        .createQuery("FROM NotePropertyIndex i WHERE i.note.id = :noteId", NotePropertyIndex.class)
        .setParameter("noteId", note.getId())
        .setFlushMode(FlushModeType.COMMIT)
        .getResultList()
        .forEach(entityManager::detach);
    // Inline deletion permits FlushModeType.COMMIT, avoiding the extra flush from the usual
    // repository @Modifying @Query.
    entityManager
        .createQuery("DELETE FROM NotePropertyIndex i WHERE i.note.id = :noteId")
        .setParameter("noteId", note.getId())
        .setFlushMode(FlushModeType.COMMIT)
        .executeUpdate();
    // Cascade persist makes still-transient authoredNoteReferenceRows readable before flush,
    // preventing TransientPropertyValueException.
    entityManager.persist(note);
    NoteContentMarkdown.splitLeadingFrontmatter(note.getContent() == null ? "" : note.getContent())
        .ifPresent(
            lf -> {
              Map<String, AuthoredNoteReferenceRow> bySourceLocalKey =
                  note.authoredReferenceRowsBySourceLocalKey();
              NotePropertyIndexPlanner.plannedRows(lf.frontmatter(), canonicalDonutOrigin)
                  .forEach(planned -> saveIndexRow(note, planned, bySourceLocalKey));
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

    Map<String, AuthoredNoteReferenceRow> authoredRows =
        note.authoredReferenceRowsBySourceLocalKey();
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

  private void saveIndexRow(
      Note indexOwner,
      NotePropertyIndexPlanner.PlannedRow planned,
      Map<String, AuthoredNoteReferenceRow> bySourceLocalKey) {
    NotePropertyIndex row = new NotePropertyIndex();
    row.setNote(indexOwner);
    row.setPropertyKey(planned.propertyKey());
    row.setItemIndex(planned.itemIndex());
    row.setPropertyValue(planned.propertyValue());
    resolveAuthoredReference(planned, bySourceLocalKey).ifPresent(row::setAuthoredNoteReference);
    notePropertyIndexRepository.save(row);
  }

  private Optional<AuthoredNoteReferenceRow> resolveAuthoredReference(
      NotePropertyIndexPlanner.PlannedRow planned,
      Map<String, AuthoredNoteReferenceRow> bySourceLocalKey) {
    return Optional.ofNullable(planned.sourceLocalKey()).map(bySourceLocalKey::get);
  }
}
