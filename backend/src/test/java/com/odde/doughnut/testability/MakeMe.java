package com.odde.doughnut.testability;

import com.odde.doughnut.testability.builders.NoteBuilder;
import com.odde.doughnut.testability.builders.UserBuilder;
import org.springframework.stereotype.Component;

@Component
public class MakeMe {
    public UserBuilder aUser() {
        return new UserBuilder(this);
    }

    public NoteBuilder aNote() {
        return new NoteBuilder(this);
    }
}
