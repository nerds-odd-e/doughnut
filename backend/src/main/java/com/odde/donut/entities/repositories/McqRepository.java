package com.odde.donut.entities.repositories;

import com.odde.donut.entities.Mcq;
import java.util.List;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;

public interface McqRepository extends CrudRepository<Mcq, Integer> {
  @Modifying(flushAutomatically = true)
  @Query(
      "DELETE FROM Mcq question WHERE question.id IN :questionIds "
          + "AND NOT EXISTS (SELECT prompt.id FROM RecallPrompt prompt WHERE prompt.mcq = question)")
  void deleteUnreferencedByIds(@Param("questionIds") List<Integer> questionIds);

  @Query(
      value =
          """
          SELECT m.* FROM mcq m
          WHERE EXISTS (
            SELECT 1 FROM recall_prompt rp
            JOIN memory_tracker mt ON mt.id = rp.memory_tracker_id
            WHERE rp.mcq_id = m.id AND mt.note_id = :noteId
              AND (:propertyKey IS NULL OR (
                CAST(mt.property_key AS BINARY) = CAST(:propertyKey AS BINARY)
                AND (:propertyValue IS NULL
                  OR CAST(mt.property_value AS BINARY) = CAST(:propertyValue AS BINARY))))
          )
          ORDER BY m.created_at DESC, m.id DESC LIMIT 10
          """,
      nativeQuery = true)
  List<Mcq> findRecentForNote(
      @Param("noteId") Integer noteId,
      @Param("propertyKey") String propertyKey,
      @Param("propertyValue") String propertyValue);
}
