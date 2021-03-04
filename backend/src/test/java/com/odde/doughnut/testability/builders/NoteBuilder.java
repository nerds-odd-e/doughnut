package com.odde.doughnut.testability.builders;

import com.odde.doughnut.entities.NoteEntity;
import com.odde.doughnut.entities.UserEntity;
import com.odde.doughnut.models.UserModel;
import com.odde.doughnut.services.ModelFactoryService;
import com.odde.doughnut.testability.MakeMe;

import java.time.LocalDate;
import java.util.Date;

public class NoteBuilder {
    static TestObjectCounter titleCounter = new TestObjectCounter(n->"title" + n);
    private final MakeMe makeMe;
    private NoteEntity note = new NoteEntity();

    public NoteBuilder(MakeMe makeMe){
        this.makeMe = makeMe;
        note.setTitle(titleCounter.generate());
        note.setDescription("descrption");
        note.setUpdatedDatetime(java.sql.Date.valueOf(LocalDate.now()));
    }
    public NoteEntity inMemoryPlease() {
        return note;
    }

    public NoteBuilder forUser(UserEntity userEntity) {
        note.setUserEntity(userEntity);
        return this;
    }

    public NoteBuilder forUser(UserModel userModel) {
        note.setUserEntity(userModel.getUserEntity());
        return this;
    }

    public NoteBuilder under(NoteEntity parentNote) {
        note.setParentNote(parentNote);
        parentNote.getChildren().add(note);
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

    public NoteEntity please() {
        makeMe.modelFactoryService.noteRepository.save(note);
        return note;
    }
}
