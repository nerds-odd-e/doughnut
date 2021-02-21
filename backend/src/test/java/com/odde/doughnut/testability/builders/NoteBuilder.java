package com.odde.doughnut.testability.builders;

import com.odde.doughnut.models.Note;
import com.odde.doughnut.models.User;
import org.hibernate.Session;

import java.util.Date;

public class NoteBuilder {
    private final Session hibernateSession;
    private Note note = new Note();

    public NoteBuilder(Session session){
        hibernateSession = session;
        note.setTitle("title");
        note.setDescription("descrption");
    }
    public Note inMemoryPlease() {
        return note;
    }

    public NoteBuilder forUser(User user) {
        note.setUser(user);
        hibernateSession.refresh(user);
        return this;
    }

    public Note please() {
        hibernateSession.save(note);
        return note;
    }

    public NoteBuilder updatedAt(Date updatedDatetime) {
        note.setUpdatedDatetime(updatedDatetime);
        return this;
    }
}
