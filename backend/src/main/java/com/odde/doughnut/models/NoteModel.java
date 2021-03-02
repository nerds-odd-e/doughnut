package com.odde.doughnut.models;

import com.odde.doughnut.entities.NoteEntity;
import com.odde.doughnut.services.ModelFactoryService;
import org.apache.logging.log4j.util.Strings;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

public class NoteModel {

    private final NoteEntity note;
    private final ModelFactoryService modelFactoryService;

    public NoteModel(NoteEntity note, ModelFactoryService modelFactoryService) {
        this.note = note;
        this.modelFactoryService = modelFactoryService;
    }

    public String getTitle() {
        return note.getTitle();
    }

    public void linkNote(NoteEntity targetNote) {
        note.linkToNote(targetNote);
        note.setUpdatedDatetime(new Date());
        modelFactoryService.noteRepository.save(note);
    }


}
