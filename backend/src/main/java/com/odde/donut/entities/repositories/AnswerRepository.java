package com.odde.donut.entities.repositories;

import com.odde.donut.entities.Answer;
import java.sql.Timestamp;
import java.util.List;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;

public interface AnswerRepository extends CrudRepository<Answer, Integer> {
  /**
   * The learner's answers from {@code from} that come after the position ({@code beforeAt}, {@code
   * beforeId}), newest first by answer time, then answer ID. An answer is the learner's through a
   * recall log or, for an Overlap that records none, through its prompt.
   */
  @Query(
      """
      SELECT a FROM Answer a
      WHERE a.createdAt >= :from
        AND (a.createdAt < :beforeAt OR (a.createdAt = :beforeAt AND a.id < :beforeId))
        AND (EXISTS (
            SELECT rl FROM RecallLog rl
            WHERE rl.answer = a AND rl.memoryTracker.user.id = :userId)
          OR EXISTS (
            SELECT rp FROM RecallPrompt rp
            WHERE rp.answer = a AND rp.memoryTracker.user.id = :userId))
      ORDER BY a.createdAt DESC, a.id DESC
      """)
  List<Answer> findRecordedAnswersOfLearnerBefore(
      @Param("userId") Integer userId,
      @Param("from") Timestamp from,
      @Param("beforeAt") Timestamp beforeAt,
      @Param("beforeId") Integer beforeId,
      Pageable pageable);
}
