package com.odde.doughnut.testability.builders;

import com.odde.doughnut.models.Note;
import com.odde.doughnut.models.User;
import com.odde.doughnut.testability.MakeMe;
import org.hibernate.Session;

import java.time.LocalDate;
import java.util.Date;

public class NoteBuilder {
    static TestObjectCounter titleCounter = new TestObjectCounter(n->"title" + n);
    private final Session hibernateSession;
    private Note note = new Note();

    public NoteBuilder(Session session, MakeMe makeMe){
        hibernateSession = session;
        note.setTitle(titleCounter.generate());
        note.setDescription("descrption");
        note.setUpdatedDatetime(java.sql.Date.valueOf(LocalDate.now()));
    }
    public Note inMemoryPlease() {
        return note;
    }

    public NoteBuilder forUser(User user) {
        note.setUser(user);
        if (user.getId() != null) {
            hibernateSession.refresh(user);
        }
        return this;
    }

    public NoteBuilder updatedAt(Date updatedDatetime) {
        note.setUpdatedDatetime(updatedDatetime);
        return this;
    }

    public Note please() {
        hibernateSession.save(note);
        return note;
    }
}
