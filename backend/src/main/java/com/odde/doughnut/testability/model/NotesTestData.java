package com.odde.doughnut.testability.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.odde.doughnut.algorithms.NoteContentMarkdown;
import com.odde.doughnut.entities.DisplayName;
import com.odde.doughnut.entities.Note;
import com.odde.doughnut.entities.Notebook;
import com.odde.doughnut.factoryServices.EntityPersister;
import io.swagger.v3.oas.annotations.media.Schema;
import java.sql.Timestamp;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import lombok.Getter;
import lombok.Setter;
import org.apache.logging.log4j.util.Strings;

@Schema(name = "NotesTestData")
public class NotesTestData {
  @Getter @Setter private List<NoteTestData> noteTestData;
  @Getter @Setter private String externalIdentifier;
  @Getter @Setter private String circleName;

  @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
  @Getter
  @Setter
  private String notebookName;

  public Map<String, Note> buildIndividualNotes(Timestamp currentUTCTimestamp) {
    Map<String, Note> titleNoteMap = new LinkedHashMap<>();
    for (NoteTestData noteTestData : noteTestData) {
      titleNoteMap.put(noteTestData.title, noteTestData.buildNote(currentUTCTimestamp));
    }
    return titleNoteMap;
  }

  public void buildNoteTree(
      Notebook notebook,
      Timestamp currentUTCTimestamp,
      Map<String, Note> titleNoteMap,
      EntityPersister entityPersister) {
    for (NoteTestData injection : noteTestData) {
      Note note = titleNoteMap.get(injection.title);
      note.initializeNewNote(notebook, currentUTCTimestamp, injection.title);
      notebook.setUpdatedAt(currentUTCTimestamp);
      entityPersister.merge(notebook);
    }
  }

  public void saveByOriginalOrder(Map<String, Note> titleNoteMap, EntityPersister entityPersister) {
    noteTestData.forEach(inject -> entityPersister.save(titleNoteMap.get(inject.title)));
  }

  @Schema(name = "NoteTestData")
  public static class NoteTestData {
    @JsonProperty("Title")
    public String title;

    @JsonProperty("Content")
    @Setter
    private String content;

    @JsonProperty("Skip Memory Tracking")
    @Getter
    @Setter
    private Boolean skipMemoryTracking;

    @JsonProperty("Remember Spelling")
    @Setter
    private Boolean rememberSpelling;

    @JsonProperty("Image Url")
    @Setter
    private String imageUrl;

    @JsonProperty("Image Mask")
    @Setter
    private String imageMask;

    @Schema(
        name = "Folder",
        description =
            "Notebook-local folder path (segments separated by /). E2E/testability only: missing"
                + " folder rows are created here, then the note is assigned that folder. Production"
                + " note APIs do not accept or infer folder paths.")
    @JsonProperty("Folder")
    @Getter
    @Setter
    private String folder;

    private Note buildNote(Timestamp currentUTCTimestamp) {
      Note note = new Note();
      note.setTitle(new DisplayName(title));
      note.setContent(content);
      note.setUpdatedAt(currentUTCTimestamp);
      if (rememberSpelling != null) {
        note.getRecallSetting().setRememberSpelling(rememberSpelling);
      }

      String url = imageUrl != null ? imageUrl.trim() : "";
      boolean hasImage = !Strings.isBlank(url);
      String mask = imageMask != null ? imageMask : "";
      note.setContent(
          NoteContentMarkdown.mergeNoteImageScalarsIntoContent(
              note.getContent() != null ? note.getContent() : "", hasImage, url, mask));

      note.setUpdatedAt(currentUTCTimestamp);
      return note;
    }
  }
}
