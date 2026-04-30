package com.odde.doughnut.services;

import com.odde.doughnut.controllers.dto.AdminDataMigrationStatusDTO;
import com.odde.doughnut.entities.Note;
import com.odde.doughnut.entities.repositories.NoteRepository;
import com.odde.doughnut.factoryServices.EntityPersister;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AdminDataMigrationService {

  public static final String READY_MESSAGE =
      "Wiki data migration: relationship title and details backfill, then note slug regeneration.";

  private final NoteRepository noteRepository;
  private final WikiSlugPathService wikiSlugPathService;
  private final EntityPersister entityPersister;

  public AdminDataMigrationService(
      NoteRepository noteRepository,
      WikiSlugPathService wikiSlugPathService,
      EntityPersister entityPersister) {
    this.noteRepository = noteRepository;
    this.wikiSlugPathService = wikiSlugPathService;
    this.entityPersister = entityPersister;
  }

  public AdminDataMigrationStatusDTO getStatus() {
    AdminDataMigrationStatusDTO dto = new AdminDataMigrationStatusDTO();
    dto.setMessage(READY_MESSAGE);
    return dto;
  }

  @Transactional
  public AdminDataMigrationStatusDTO runBatch() {
    int titlesBackfilled = backfillRelationshipTitles();
    int detailsBackfilled = backfillRelationshipDetails();
    entityPersister.flush();
    wikiSlugPathService.regenerateAllNoteSlugPaths();
    entityPersister.flush();
    AdminDataMigrationStatusDTO dto = new AdminDataMigrationStatusDTO();
    dto.setMessage(
        ("Migration batch: backfilled %d relationship note title(s), %d relationship note detail(s);"
                + " regenerated note slugs.")
            .formatted(titlesBackfilled, detailsBackfilled));
    return dto;
  }

  private int backfillRelationshipTitles() {
    int count = 0;
    for (Note relation : noteRepository.findRelationshipNotesWithBlankTitleForMigration()) {
      relation.setTitle(
          RelationshipNoteTitleFormatter.format(
              relation.getParent().getTitle(),
              relation.getRelationType().label,
              relation.getTargetNote().getTitle()));
      entityPersister.merge(relation);
      count++;
    }
    return count;
  }

  private int backfillRelationshipDetails() {
    int count = 0;
    for (Note relation : noteRepository.findRelationshipNotesNeedingDetailsMigration()) {
      relation.setDetails(
          RelationshipNoteMarkdownFormatter.format(
              relation.getRelationType(),
              relation.getParent().getTitle(),
              relation.getTargetNote().getTitle(),
              relation.getDetails()));
      entityPersister.merge(relation);
      count++;
    }
    return count;
  }
}
