package com.odde.donut.testability;

import jakarta.persistence.EntityManager;

/** Deletes a committed user and the fixture graph hanging off that user. */
public final class CommittedUserCleanup {

  private CommittedUserCleanup() {}

  public static void deleteByUserExternalIdentifierLike(
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
            "DELETE b FROM bazaar_notebook b "
                + "INNER JOIN notebook nb ON b.notebook_id = nb.id "
                + "INNER JOIN ownership o ON nb.ownership_id = o.id "
                + "INNER JOIN user u ON o.user_id = u.id "
                + "WHERE u.external_identifier LIKE :like")
        .setParameter("like", externalIdentifierLike)
        .executeUpdate();
    entityManager
        .createNativeQuery(
            "DELETE bcb FROM book_content_block bcb "
                + "INNER JOIN book_block bb ON bcb.book_block_id = bb.id "
                + "INNER JOIN book b ON bb.book_id = b.id "
                + "INNER JOIN notebook nb ON b.notebook_id = nb.id "
                + "INNER JOIN ownership o ON nb.ownership_id = o.id "
                + "INNER JOIN user u ON o.user_id = u.id "
                + "WHERE u.external_identifier LIKE :like")
        .setParameter("like", externalIdentifierLike)
        .executeUpdate();
    entityManager
        .createNativeQuery(
            "DELETE a FROM notebook_attachment a "
                + "INNER JOIN notebook nb ON a.notebook_id = nb.id "
                + "INNER JOIN ownership o ON nb.ownership_id = o.id "
                + "INNER JOIN user u ON o.user_id = u.id "
                + "WHERE u.external_identifier LIKE :like")
        .setParameter("like", externalIdentifierLike)
        .executeUpdate();
    // fk_folder_parent restricts, so each pass removes only folders without child folders.
    int removedFolders;
    do {
      removedFolders =
          entityManager
              .createNativeQuery(
                  "DELETE f FROM folder f "
                      + "LEFT JOIN folder child ON child.parent_folder_id = f.id "
                      + "INNER JOIN notebook nb ON f.notebook_id = nb.id "
                      + "INNER JOIN ownership o ON nb.ownership_id = o.id "
                      + "INNER JOIN user u ON o.user_id = u.id "
                      + "WHERE child.id IS NULL AND u.external_identifier LIKE :like")
              .setParameter("like", externalIdentifierLike)
              .executeUpdate();
    } while (removedFolders > 0);
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
