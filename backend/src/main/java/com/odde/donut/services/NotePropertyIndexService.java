package com.odde.donut.services;

import com.odde.donut.algorithms.AuthoredNoteReference;
import com.odde.donut.algorithms.CanonicalDonutOrigin;
import com.odde.donut.algorithms.NoteContentMarkdown;
import com.odde.donut.algorithms.NotePropertyIndexPlanner;
import com.odde.donut.algorithms.NotePropertyIndexPlanner.ValueKey;
import com.odde.donut.entities.AuthoredNoteReferenceRow;
import com.odde.donut.entities.Note;
import com.odde.donut.entities.NotePropertyIndex;
import com.odde.donut.entities.repositories.NotePropertyIndexRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.FlushModeType;
import jakarta.persistence.PersistenceContext;
import java.sql.Timestamp;
import java.time.Instant;
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
    // Rows are deleted and reinserted (a reorder would otherwise collide on the unique item
    // index); a value that survives by (key, value) carries its change date to its new row.
    Map<ValueKey, Timestamp> changedAtByValue = new HashMap<>();
    // Detach any rows already managed in this transaction before the bulk delete, which
    // acts directly on the database and would otherwise leave stale managed copies behind.
    entityManager
        .createQuery("FROM NotePropertyIndex i WHERE i.note.id = :noteId", NotePropertyIndex.class)
        .setParameter("noteId", note.getId())
        .setFlushMode(FlushModeType.COMMIT)
        .getResultList()
        .forEach(row -> {
          changedAtByValue.put(row.valueKey(), row.getChangedAt());
          entityManager.detach(row);
        });
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
        .ifPresent(lf -> {
          Map<String, AuthoredNoteReferenceRow> bySourceLocalKey =
              note.authoredReferenceRowsBySourceLocalKey();
          NotePropertyIndexPlanner.plannedRows(lf.frontmatter(), canonicalDonutOrigin)
              .forEach(planned -> saveIndexRow(note, planned, bySourceLocalKey, changedAtByValue));
        });
  }

  /**
   * Dates a value's index rows at its commit time, never earlier than the note's creation; a value
   * present at the history's root ({@code committedAt} empty) takes the note's creation time.
   */
  public void writeValueDate(Note note, ValueKey valueKey, Optional<Instant> committedAt) {
    Timestamp createdAt = note.getCreatedAt();
    Timestamp changedAt =
        committedAt.map(Timestamp::from).filter(createdAt::before).orElse(createdAt);
    notePropertyIndexRepository
        .findByNote_IdAndPropertyKeyAndPropertyValue(
            note.getId(), valueKey.propertyKey(), valueKey.propertyValue())
        .forEach(row -> row.setChangedAt(changedAt));
  }

  /**
   * Dates, at {@code changedAt}, the values the note's content gained or changed since {@code
   * previousContent}; unchanged values keep their dates.
   */
  public void dateChangedValues(Note note, String previousContent, Instant changedAt) {
    NotePropertyIndexPlanner.changedValues(
            NoteContentMarkdown.leadingFrontmatter(previousContent),
            NoteContentMarkdown.leadingFrontmatter(note.getContent()))
        .forEach(valueKey -> writeValueDate(note, valueKey, Optional.of(changedAt)));
  }

  public List<AuthoredNoteReference> authoredReferencesForPropertyValue(
      Note note, String propertyKey, String propertyValue, List<NotePropertyIndex> indexRows) {
    List<String> plannedSourceLocalKeys = plannedSourceLocalKeys(note, propertyKey, propertyValue);
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

  private List<String> plannedSourceLocalKeys(Note note, String propertyKey, String propertyValue) {
    return NotePropertyIndexPlanner.plannedRows(
            NoteContentMarkdown.leadingFrontmatter(note.getContent()), canonicalDonutOrigin)
        .stream()
        .filter(planned ->
            planned.propertyKey().equals(propertyKey)
                && planned.propertyValue().equals(propertyValue))
        .map(NotePropertyIndexPlanner.PlannedRow::sourceLocalKey)
        .filter(Objects::nonNull)
        .distinct()
        .toList();
  }

  private void saveIndexRow(
      Note indexOwner,
      NotePropertyIndexPlanner.PlannedRow planned,
      Map<String, AuthoredNoteReferenceRow> bySourceLocalKey,
      Map<ValueKey, Timestamp> changedAtByValue) {
    NotePropertyIndex row = new NotePropertyIndex();
    row.setNote(indexOwner);
    row.setPropertyKey(planned.propertyKey());
    row.setItemIndex(planned.itemIndex());
    row.setPropertyValue(planned.propertyValue());
    row.setChangedAt(changedAtByValue.get(planned.valueKey()));
    resolveAuthoredReference(planned, bySourceLocalKey).ifPresent(row::setAuthoredNoteReference);
    notePropertyIndexRepository.save(row);
  }

  private Optional<AuthoredNoteReferenceRow> resolveAuthoredReference(
      NotePropertyIndexPlanner.PlannedRow planned,
      Map<String, AuthoredNoteReferenceRow> bySourceLocalKey) {
    return Optional.ofNullable(planned.sourceLocalKey()).map(bySourceLocalKey::get);
  }
}
