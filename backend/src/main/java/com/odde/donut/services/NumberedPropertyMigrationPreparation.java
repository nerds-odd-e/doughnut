package com.odde.donut.services;

import com.odde.donut.algorithms.NoteContentMarkdown;
import com.odde.donut.algorithms.NoteContentMarkdown.ConsolidatedProperties;
import com.odde.donut.entities.Note;
import com.odde.donut.entities.repositories.NoteRepository;
import com.odde.donut.factoryServices.EntityPersister;
import com.odde.donut.services.PropertyMemoryTrackerService.ConsolidatedFocuses;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Computes the complete proposed authored content and learning mappings without mutating them. */
@Service
class NumberedPropertyMigrationPreparation {
  private final NoteRepository notes;
  private final PropertyMemoryTrackerService trackers;
  private final NumberedPropertyReferencePreservation references;
  private final EntityPersister entityPersister;

  NumberedPropertyMigrationPreparation(
      NoteRepository notes,
      PropertyMemoryTrackerService trackers,
      NumberedPropertyReferencePreservation references,
      EntityPersister entityPersister) {
    this.notes = notes;
    this.trackers = trackers;
    this.references = references;
    this.entityPersister = entityPersister;
  }

  record Change(Note note, String content, ConsolidatedFocuses mapping) {}

  record Prepared(List<Change> changes, Set<Integer> notebookIds, String diagnostic) {}

  @Transactional(readOnly = true)
  public Prepared prepare(Integer notebookId) {
    return entityPersister.readWithoutAutoFlush(() -> prepareUnflushed(notebookId));
  }

  private Prepared prepareUnflushed(Integer notebookId) {
    Map<Integer, ConsolidatedProperties> projected = new LinkedHashMap<>();
    for (Note note : notes.findAllByNotebookIdOrderByIdAsc(notebookId)) {
      var transformed = NoteContentMarkdown.consolidateNumberedProperties(note.getContent());
      if (transformed.diagnostic() != null) return refused(note, transformed.diagnostic());
      if (!transformed.sourceKeys().isEmpty()) projected.put(note.getId(), transformed);
    }
    List<Change> changes = new ArrayList<>();
    Set<Integer> notebookIds = new LinkedHashSet<>(Set.of(notebookId));
    if (!projected.isEmpty()) {
      var check = references.check(notebookId, projected);
      for (Note source : notes.findAll()) {
        if (!check.mayAffect(source)) continue;
        var rewrites = check.prepare(source);
        if (rewrites.diagnostic() != null) return refused(source, rewrites.diagnostic());
        var transformed =
            projected.getOrDefault(
                source.getId(),
                new ConsolidatedProperties(source.getContent(), Map.of(), Set.of(), null));
        var mapping = trackers.composeConsolidatedFocuses(source, transformed, rewrites);
        String diagnostic =
            trackers.consolidationDiagnostic(source, mapping.focuses(), mapping.sourceKeys());
        if (diagnostic != null) return refused(source, diagnostic);
        String content = rewrites.apply(transformed.content());
        if (!Objects.equals(content, source.getContent())) {
          changes.add(new Change(source, content, mapping));
          notebookIds.add(source.getNotebook().getId());
        }
      }
    }
    return new Prepared(List.copyOf(changes), Set.copyOf(notebookIds), null);
  }

  private static Prepared refused(Note note, String diagnostic) {
    return new Prepared(List.of(), Set.of(), "Note " + note.getId() + ": " + diagnostic);
  }
}
