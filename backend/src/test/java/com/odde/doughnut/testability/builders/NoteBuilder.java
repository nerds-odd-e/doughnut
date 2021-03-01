package com.odde.doughnut.testability.builders;

import com.odde.doughnut.entities.NoteEntity;
import com.odde.doughnut.entities.User;
import com.odde.doughnut.services.ModelFactoryService;
import com.odde.doughnut.testability.MakeMe;

import java.time.LocalDate;
import java.util.Date;

public class NoteBuilder {
    static TestObjectCounter titleCounter = new TestObjectCounter(n->"title" + n);
    private NoteEntity note = new NoteEntity();

    public NoteBuilder(MakeMe makeMe){
        note.setTitle(titleCounter.generate());
        note.setDescription("descrption");
        note.setUpdatedDatetime(java.sql.Date.valueOf(LocalDate.now()));
    }
    public NoteEntity inMemoryPlease() {
        return note;
    }

    public NoteBuilder forUser(User user) {
        note.setUser(user);
        return this;
    }

    public NoteBuilder under(NoteEntity parentNote) {
        parentNote.addChild(note);
        return this;
    }

    public NoteBuilder linkTo(NoteEntity referTo) {
        note.linkToNote(referTo);
        return this;
    }

    public NoteBuilder updatedAt(Date updatedDatetime) {
        note.setUpdatedDatetime(updatedDatetime);
        return this;
    }

    public NoteEntity please(ModelFactoryService modelFactoryService) {
        modelFactoryService.noteRepository.save(note);
        return note;
    }
}
