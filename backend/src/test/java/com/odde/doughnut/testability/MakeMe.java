package com.odde.doughnut.testability;

import com.odde.doughnut.entities.NoteEntity;
import com.odde.doughnut.models.ModelForEntity;
import com.odde.doughnut.services.ModelFactoryService;
import com.odde.doughnut.testability.builders.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.validation.BindingResult;

import java.nio.CharBuffer;

@Component
public class MakeMe {
    @Autowired public ModelFactoryService modelFactoryService;

    public UserBuilder aUser() {
        return new UserBuilder(this);
    }

    public NoteBuilder aNote() {
        return new NoteBuilder(this);
    }

    public BazaarNoteBuilder aBazaarNode(NoteEntity note) {
        return new BazaarNoteBuilder(this, note);
    }

    public <T> T refresh(T object) {
        if (object instanceof ModelForEntity) {
            modelFactoryService.entityManager.refresh(((ModelForEntity<?>) object).getEntity());
        }
        else {
            modelFactoryService.entityManager.refresh(object);
        }
        return object;
    }

    public String aStringOfLength(int length, char withChar) {
        return CharBuffer.allocate(length).toString().replace('\0', withChar);
    }

    public String aStringOfLength(int length) {
        return aStringOfLength(length, 'a');
    }

    public BindingResult successfulBindingResult() {
        return new FakeBindingResult(false);
    }

    public BindingResult failedBindingResult() {
        return new FakeBindingResult(true);
    }

    public ReviewPointBuilder aReviewPointFor(NoteEntity noteEntity) {
        return new ReviewPointBuilder(noteEntity, this);
    }

    public TimestampBuilder aTimestamp() {
        return new TimestampBuilder();
    }
}
