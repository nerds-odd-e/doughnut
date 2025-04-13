package com.odde.doughnut.models;

import java.util.ArrayList;
import java.util.List;

public class ImportResult {
  private List<NotebookImportResult> notebooks = new ArrayList<>();
  private int totalImported;
  private List<String> warnings = new ArrayList<>();

  public static class NotebookImportResult {
    private Long originalId;
    private Long newId;
    private String title;
    private int notesCount;

    public Long getOriginalId() {
      return originalId;
    }

    public void setOriginalId(Long originalId) {
      this.originalId = originalId;
    }

    public Long getNewId() {
      return newId;
    }

    public void setNewId(Long newId) {
      this.newId = newId;
    }

    public String getTitle() {
      return title;
    }

    public void setTitle(String title) {
      this.title = title;
    }

    public int getNotesCount() {
      return notesCount;
    }

    public void setNotesCount(int notesCount) {
      this.notesCount = notesCount;
    }
  }

  public List<NotebookImportResult> getNotebooks() {
    return notebooks;
  }

  public void setNotebooks(List<NotebookImportResult> notebooks) {
    this.notebooks = notebooks;
  }

  public int getTotalImported() {
    return totalImported;
  }

  public void setTotalImported(int totalImported) {
    this.totalImported = totalImported;
  }

  public List<String> getWarnings() {
    return warnings;
  }

  public void setWarnings(List<String> warnings) {
    this.warnings = warnings;
  }

  public void addNotebook(NotebookImportResult notebook) {
    notebooks.add(notebook);
    totalImported++;
  }

  public void addWarning(String warning) {
    warnings.add(warning);
  }
}
