package com.odde.doughnut.testability;

import com.odde.doughnut.testability.builders.NoteBuilder;
import com.odde.doughnut.testability.builders.UserBuilder;
import org.hibernate.Session;

public class MakeMe {

    private Session session;

    public MakeMe(Session session) {
        this.session = session;
    }

    public UserBuilder aUser() {
        return new UserBuilder(session);
    }

    public NoteBuilder aNote() {
        return new NoteBuilder(session);
    }
}
