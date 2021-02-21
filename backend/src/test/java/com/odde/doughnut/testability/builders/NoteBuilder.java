package com.odde.doughnut.testability.builders;

import com.odde.doughnut.models.Note;
import com.odde.doughnut.models.User;
import org.hibernate.Session;

public class NoteBuilder {
    private Note note = new Note();

    public NoteBuilder(){
        note.setTitle("title");
        note.setDescription("descrption");
    }
    public Note inMemoryPlease() {
        return note;
    }

    public NoteBuilder forUser(User user) {
        note.setUser(user);
        return this;
    }

    public Note please(Session session) {
        session.save(note);
        return note;
    }
}
