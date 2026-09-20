package com.odde.donut.entities.repositories;

import com.odde.donut.entities.NotebookAttachment;
import com.odde.donut.services.notebookExport.ExportAttachmentRow;
import java.util.List;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;

public interface NotebookAttachmentRepository extends CrudRepository<NotebookAttachment, Integer> {

  @Query(
      """
      SELECT NEW com.odde.donut.services.notebookExport.ExportAttachmentRow(a.filename, a.content)
      FROM NotebookAttachment a WHERE a.notebook.id = :notebookId ORDER BY a.id ASC
      """)
  List<ExportAttachmentRow> findExportRowsByNotebookId(@Param("notebookId") Integer notebookId);

  List<NotebookAttachment> findByNotebook_Id(Integer notebookId);
}
