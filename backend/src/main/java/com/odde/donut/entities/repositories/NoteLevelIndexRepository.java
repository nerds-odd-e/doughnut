package com.odde.donut.entities.repositories;

import com.odde.donut.entities.NoteLevelIndex;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface NoteLevelIndexRepository extends JpaRepository<NoteLevelIndex, Integer> {

  /**
   * Race-safe create-or-update for a note's level row: concurrent first refreshes of the same note
   * race on the {@code note_id} unique key, so the insert itself must resolve the conflict instead
   * of a caller pre-checking existence.
   */
  @Modifying
  @Query(
      value =
          "INSERT INTO note_level_index (note_id, level) VALUES (:noteId, :level) AS incoming "
              + "ON DUPLICATE KEY UPDATE level = incoming.level",
      nativeQuery = true)
  void upsertLevel(@Param("noteId") Integer noteId, @Param("level") Integer level);
}
