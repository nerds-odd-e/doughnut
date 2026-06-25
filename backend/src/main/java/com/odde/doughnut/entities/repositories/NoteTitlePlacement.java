package com.odde.doughnut.entities.repositories;

/**
 * Content-free view of a note used by the title-alias migration to compute planned titles and
 * detect collisions across the whole corpus without hydrating full {@code Note} entities.
 */
public record NoteTitlePlacement(Integer id, String title, Integer notebookId, Integer folderId) {}
