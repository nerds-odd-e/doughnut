package com.odde.doughnut.models;

import com.odde.doughnut.entities.Notebook;
import com.odde.doughnut.entities.User;
import com.odde.doughnut.entities.json.NotebookViewedByUser;
import com.odde.doughnut.entities.json.NotebooksViewedByUser;

import java.util.List;
import java.util.stream.Collectors;

public class JsonViewer {
    private User viewer;

    public JsonViewer(User viewer) {

        this.viewer = viewer;
    }

    public NotebookViewedByUser jsonNotebookViewedByUser(Notebook notebook) {
        NotebookViewedByUser notebookViewedByUser = new NotebookViewedByUser();
        notebookViewedByUser.setId(notebook.getId());
        notebookViewedByUser.setHeadNoteId(notebook.getHeadNote().getId());
        notebookViewedByUser.setHeadNote(notebook.getHeadNote());
        notebookViewedByUser.setOwnership(notebook.getOwnership());
        notebookViewedByUser.setFromBazaar(viewer == null || !viewer.owns(notebook));
        return notebookViewedByUser;
    }

    public NotebooksViewedByUser jsonNotebooksViewedByUser(List<Notebook> allNotebooks) {
      NotebooksViewedByUser notebooksViewedByUser = new NotebooksViewedByUser();
      notebooksViewedByUser.notebooks = allNotebooks.stream().map(this::jsonNotebookViewedByUser).collect(Collectors.toList());
      return notebooksViewedByUser;
    }
}
