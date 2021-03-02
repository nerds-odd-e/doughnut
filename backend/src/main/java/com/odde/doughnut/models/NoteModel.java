package com.odde.doughnut.models;

import com.odde.doughnut.entities.NoteEntity;
import com.odde.doughnut.services.ModelFactoryService;
import org.apache.logging.log4j.util.Strings;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

public class NoteModel extends TreeNodeModel {

    public NoteModel(NoteEntity note, ModelFactoryService modelFactoryService) {
        super(note, modelFactoryService);
    }

    public String getTitle() {
        return note.getTitle();
    }

    public List<String> getDescriptionLines() {
        if (Strings.isEmpty(note.getDescription())) {
            return new ArrayList<>();
        }
        return Arrays.asList(note.getDescription().split("\n"));
    }

    public void linkNote(NoteEntity targetNote) {
        note.linkToNote(targetNote);
        note.setUpdatedDatetime(new Date());
        noteRepository.save(note);
    }


}
