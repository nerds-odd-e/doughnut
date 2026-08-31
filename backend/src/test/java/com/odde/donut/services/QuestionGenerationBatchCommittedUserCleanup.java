package com.odde.donut.services;

import jakarta.persistence.EntityManager;

final class QuestionGenerationBatchCommittedUserCleanup {

  private QuestionGenerationBatchCommittedUserCleanup() {}

  static void deleteByUserExternalIdentifierLike(
      EntityManager entityManager, String externalIdentifierLike) {
    entityManager
        .createNativeQuery(
            "DELETE rp FROM recall_prompt rp "
                + "INNER JOIN memory_tracker mt ON rp.memory_tracker_id = mt.id "
                + "INNER JOIN user u ON mt.user_id = u.id "
                + "WHERE u.external_identifier LIKE :like")
        .setParameter("like", externalIdentifierLike)
        .executeUpdate();
    entityManager
        .createNativeQuery(
            "DELETE mcq FROM mcq "
                + "INNER JOIN note n ON mcq.note_id = n.id "
                + "INNER JOIN notebook nb ON n.notebook_id = nb.id "
                + "INNER JOIN ownership o ON nb.ownership_id = o.id "
                + "INNER JOIN user u ON o.user_id = u.id "
                + "WHERE u.external_identifier LIKE :like")
        .setParameter("like", externalIdentifierLike)
        .executeUpdate();
    entityManager
        .createNativeQuery(
            "DELETE qgr FROM question_generation_batch_request qgr "
                + "INNER JOIN question_generation_batch qgb ON qgr.batch_id = qgb.id "
                + "INNER JOIN user u ON qgb.user_id = u.id "
                + "WHERE u.external_identifier LIKE :like")
        .setParameter("like", externalIdentifierLike)
        .executeUpdate();
    entityManager
        .createNativeQuery(
            "DELETE qgb FROM question_generation_batch qgb "
                + "INNER JOIN user u ON qgb.user_id = u.id "
                + "WHERE u.external_identifier LIKE :like")
        .setParameter("like", externalIdentifierLike)
        .executeUpdate();
    entityManager
        .createNativeQuery(
            "DELETE mt FROM memory_tracker mt "
                + "INNER JOIN user u ON mt.user_id = u.id "
                + "WHERE u.external_identifier LIKE :like")
        .setParameter("like", externalIdentifierLike)
        .executeUpdate();
    entityManager
        .createNativeQuery(
            "DELETE n FROM note n "
                + "INNER JOIN notebook nb ON n.notebook_id = nb.id "
                + "INNER JOIN ownership o ON nb.ownership_id = o.id "
                + "INNER JOIN user u ON o.user_id = u.id "
                + "WHERE u.external_identifier LIKE :like")
        .setParameter("like", externalIdentifierLike)
        .executeUpdate();
    entityManager
        .createNativeQuery(
            "DELETE nb FROM notebook nb "
                + "INNER JOIN ownership o ON nb.ownership_id = o.id "
                + "INNER JOIN user u ON o.user_id = u.id "
                + "WHERE u.external_identifier LIKE :like")
        .setParameter("like", externalIdentifierLike)
        .executeUpdate();
    entityManager
        .createNativeQuery(
            "DELETE o FROM ownership o "
                + "INNER JOIN user u ON o.user_id = u.id "
                + "WHERE u.external_identifier LIKE :like")
        .setParameter("like", externalIdentifierLike)
        .executeUpdate();
    entityManager
        .createNativeQuery("DELETE FROM user WHERE external_identifier LIKE :like")
        .setParameter("like", externalIdentifierLike)
        .executeUpdate();
  }
}
