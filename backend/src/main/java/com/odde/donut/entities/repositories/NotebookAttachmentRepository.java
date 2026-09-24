package com.odde.donut.entities.repositories;

import com.odde.donut.controllers.dto.NotebookAttachmentListItem;
import com.odde.donut.entities.NotebookAttachment;
import com.odde.donut.services.notebookTree.PortableTreeAttachmentRow;
import java.util.Collection;
import java.util.List;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;

public interface NotebookAttachmentRepository extends CrudRepository<NotebookAttachment, Integer> {

  @Query(
      """
      SELECT NEW com.odde.donut.services.notebookTree.PortableTreeAttachmentRow(f.id, a.filename, a.content)
      FROM NotebookAttachment a LEFT JOIN a.folder f
      WHERE a.notebook.id = :notebookId ORDER BY a.id ASC
      """)
  List<PortableTreeAttachmentRow> findPortableTreeRowsByNotebookId(
      @Param("notebookId") Integer notebookId);

  List<NotebookAttachment> findByNotebook_Id(Integer notebookId);

  @Query(
      """
      SELECT NEW com.odde.donut.controllers.dto.NotebookAttachmentListItem(a.id, a.filename)
      FROM NotebookAttachment a
      WHERE a.notebook.id = :notebookId AND a.folder IS NULL ORDER BY a.id ASC
      """)
  List<NotebookAttachmentListItem> findRootListItemsByNotebookId(
      @Param("notebookId") Integer notebookId);

  @Query(
      """
      SELECT NEW com.odde.donut.controllers.dto.NotebookAttachmentListItem(a.id, a.filename)
      FROM NotebookAttachment a
      WHERE a.folder.id = :folderId ORDER BY a.id ASC
      """)
  List<NotebookAttachmentListItem> findListItemsByFolderId(@Param("folderId") Integer folderId);

  boolean existsByFolder_IdIn(Collection<Integer> folderIds);
}
