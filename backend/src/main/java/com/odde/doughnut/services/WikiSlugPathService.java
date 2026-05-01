package com.odde.doughnut.services;

import com.odde.doughnut.entities.Folder;
import java.util.List;
import java.util.Set;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

@Service
public class WikiSlugPathService {

  private final JdbcTemplate jdbcTemplate;

  public WikiSlugPathService(JdbcTemplate jdbcTemplate) {
    this.jdbcTemplate = jdbcTemplate;
  }

  public void assignSlugForNewFolder(Folder folder) {
    Integer notebookId = folder.getNotebook().getId();
    Integer parentFolderId =
        folder.getParentFolder() == null ? null : folder.getParentFolder().getId();
    Set<String> siblingBasenames =
        notebookId == null
            ? Set.of()
            : WikiSlugPathAssignment.basenamesFromSlugs(
                findFolderSlugsOfSiblingsJdbc(notebookId, parentFolderId));
    WikiSlugPathAssignment.setFolderSlug(folder, siblingBasenames);
  }

  public void assignSlugForNewFolderSkippingSiblingQuery(Folder folder) {
    WikiSlugPathAssignment.setFolderSlug(folder, Set.of());
  }

  private List<String> findFolderSlugsOfSiblingsJdbc(Integer notebookId, Integer parentFolderId) {
    if (parentFolderId == null) {
      return jdbcTemplate.queryForList(
          "SELECT slug FROM folder WHERE notebook_id = ? AND parent_folder_id IS NULL",
          String.class,
          notebookId);
    }
    return jdbcTemplate.queryForList(
        "SELECT slug FROM folder WHERE notebook_id = ? AND parent_folder_id = ?",
        String.class,
        notebookId,
        parentFolderId);
  }
}
