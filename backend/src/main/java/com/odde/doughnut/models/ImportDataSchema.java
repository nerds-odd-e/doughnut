package com.odde.doughnut.models;

import java.time.LocalDateTime;
import java.util.List;

public class ImportDataSchema {
  private Metadata metadata;
  private List<NotebookImport> notebooks;

  public static class Metadata {
    private String version;
    private String exportedBy;
    private LocalDateTime exportedAt;

    public String getVersion() {
      return version;
    }

    public void setVersion(String version) {
      this.version = version;
    }

    public String getExportedBy() {
      return exportedBy;
    }

    public void setExportedBy(String exportedBy) {
      this.exportedBy = exportedBy;
    }

    public LocalDateTime getExportedAt() {
      return exportedAt;
    }

    public void setExportedAt(LocalDateTime exportedAt) {
      this.exportedAt = exportedAt;
    }
  }

  public static class NotebookImport {
    private String title;
    private List<NoteImport> notes;

    public String getTitle() {
      return title;
    }

    public void setTitle(String title) {
      this.title = title;
    }

    public List<NoteImport> getNotes() {
      return notes;
    }

    public void setNotes(List<NoteImport> notes) {
      this.notes = notes;
    }
  }

  public static class NoteImport {
    private String title;
    private String details;

    public String getTitle() {
      return title;
    }

    public void setTitle(String title) {
      this.title = title;
    }

    public String getDetails() {
      return details;
    }

    public void setDetails(String details) {
      this.details = details;
    }
  }

  public Metadata getMetadata() {
    return metadata;
  }

  public void setMetadata(Metadata metadata) {
    this.metadata = metadata;
  }

  public List<NotebookImport> getNotebooks() {
    return notebooks;
  }

  public void setNotebooks(List<NotebookImport> notebooks) {
    this.notebooks = notebooks;
  }
}
