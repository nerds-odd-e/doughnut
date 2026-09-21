package com.odde.donut.entities.repositories;

import com.odde.donut.entities.NotebookAttachment;
import com.odde.donut.services.notebookTree.PortableTreeAttachmentRow;
import java.util.List;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;

public interface NotebookAttachmentRepository extends CrudRepository<NotebookAttachment, Integer> {

  @Query(
      """
      SELECT NEW com.odde.donut.services.notebookTree.PortableTreeAttachmentRow(a.filename, a.content)
      FROM NotebookAttachment a WHERE a.notebook.id = :notebookId ORDER BY a.id ASC
      """)
  List<PortableTreeAttachmentRow> findPortableTreeRowsByNotebookId(
      @Param("notebookId") Integer notebookId);

  List<NotebookAttachment> findByNotebook_Id(Integer notebookId);
}
