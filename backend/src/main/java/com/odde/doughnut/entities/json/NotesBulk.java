package com.odde.doughnut.entities.json;

import com.odde.doughnut.entities.Note;
import com.odde.doughnut.models.UserModel;

import java.util.ArrayList;
import java.util.List;

public class NotesBulk {
    public NotePositionViewedByUser notePosition;
    public List<NoteViewedByUser> notes = new ArrayList<>();

    public static NotesBulk jsonNoteWithChildren(Note note, UserModel user) {
      NotesBulk notesBulk = new NotesBulk();

      notesBulk.notePosition = note.jsonNotePosition(user.getEntity());
      notesBulk.notes.add(note.jsonObjectViewedBy(user.getEntity()));
      note.getChildren().forEach(n-> notesBulk.notes.add(n.jsonObjectViewedBy(user.getEntity())));
      return notesBulk;
    }

    public static NotesBulk jsonNoteWitheDescendants(Note note, UserModel user) {
      NotesBulk notesBulk = new NotesBulk();
      notesBulk.notePosition = note.jsonNotePosition(user.getEntity());
      notesBulk.notes.add(note.jsonObjectViewedBy(user.getEntity()));
      note.traverseBreadthFirst(n-> notesBulk.notes.add(n.jsonObjectViewedBy(user.getEntity())));
      return notesBulk;
    }
}
