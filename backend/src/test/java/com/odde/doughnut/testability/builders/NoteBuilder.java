package com.odde.doughnut.testability.builders;

import com.odde.doughnut.entities.NoteEntity;
import com.odde.doughnut.entities.UserEntity;
import com.odde.doughnut.models.UserModel;
import com.odde.doughnut.testability.EntityBuilder;
import com.odde.doughnut.testability.MakeMe;

import java.time.LocalDate;
import java.util.Date;

public class NoteBuilder extends EntityBuilder<NoteEntity> {
    static final TestObjectCounter titleCounter = new TestObjectCounter(n->"title" + n);

    public NoteBuilder(MakeMe makeMe){
        super(makeMe, new NoteEntity());
        entity.setTitle(titleCounter.generate());
        entity.setDescription("descrption");
        entity.setUpdatedDatetime(java.sql.Date.valueOf(LocalDate.now()));
    }
    public NoteEntity inMemoryPlease() {
        return entity;
    }

    public NoteBuilder forUser(UserEntity userEntity) {
        entity.setUserEntity(userEntity);
        return this;
    }

    public NoteBuilder forUser(UserModel userModel) {
        entity.setUserEntity(userModel.getEntity());
        return this;
    }

    public NoteBuilder under(NoteEntity parentNote) {
        entity.setParentNote(parentNote);
        parentNote.getChildren().add(entity);
        return this;
    }

    public NoteBuilder linkTo(NoteEntity referTo) {
        entity.linkToNote(referTo);
        return this;
    }

}
