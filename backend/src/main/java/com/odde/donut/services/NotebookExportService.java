package com.odde.donut.services;

import com.odde.donut.entities.Notebook;
import com.odde.donut.services.notebookExport.NotebookLivePortableTree;
import com.odde.donut.services.notebookExport.NotebookZipBuilder;
import org.springframework.stereotype.Service;

@Service
public class NotebookExportService {
  private final NotebookLivePortableTree livePortableTree;

  public NotebookExportService(NotebookLivePortableTree livePortableTree) {
    this.livePortableTree = livePortableTree;
  }

  public byte[] exportNotebookAsZip(Notebook notebook) {
    return NotebookZipBuilder.build(livePortableTree.entriesOf(notebook));
  }

  public String exportFileName(Notebook notebook) {
    return notebook.getName() + ".zip";
  }
}
