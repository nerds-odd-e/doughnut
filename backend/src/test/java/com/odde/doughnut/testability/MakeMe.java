package com.odde.doughnut.testability;

import com.odde.doughnut.testability.builders.NoteBuilder;
import com.odde.doughnut.testability.builders.UserBuilder;

public class MakeMe {

    public MakeMe() {
    }

    public UserBuilder aUser() {
        return new UserBuilder();
    }

    public NoteBuilder aNote() {
        return new NoteBuilder();
    }
}
