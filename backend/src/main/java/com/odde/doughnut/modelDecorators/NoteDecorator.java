package com.odde.doughnut.modelDecorators;

import com.odde.doughnut.models.Note;
import com.odde.doughnut.repositories.NoteRepository;
import org.apache.logging.log4j.util.Strings;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class NoteDecorator {
    private final NoteRepository noteRepository;
    private final Note note;

    public NoteDecorator(NoteRepository noteRepository, Note note) {
        this.noteRepository = noteRepository;
        this.note = note;
    }

    public List<Note> getAncestors() {
        if (note == null) {
            return new ArrayList<>();
        }
        return noteRepository.findAncestry(note.getId().longValue());
    }

    public String getTitle() {
        return note.getTitle();
    }

    public List<String> getDescriptionLines() {
        if (Strings.isEmpty(note.getDescription())) {
            return new ArrayList<>();
        }
        return Arrays.asList(note.getDescription().split("\n"));
    }

    public Note getNextNote() {
        return note.getChildren().stream().findFirst().orElse(null);
    }

    public Note getNextSiblingNote() {
        if (note.getParentNote() != null) {
            List<Note> allByParentId = noteRepository.findAllByParentNote(note.getParentNote());
            allByParentId.remove(0);
            for (Note n: allByParentId) {
                if (n != note) {
                    return n;
                }
            }
        }
        return null;
    }

    public Note getPreviousNote() {
        Note previousSiblingNote = getPreviousSiblingNote();
        if (previousSiblingNote != null) {
            return previousSiblingNote.getChildren().stream().findFirst().orElse(null);
        }
        return null;
    }

    public Note getPreviousSiblingNote() {
        if (note.getParentNote() != null) {
            List<Note> allByParentId = noteRepository.findAllByParentNote(note.getParentNote());
            for (Note n: allByParentId) {
                if (n != note) {
                    return n;
                }
            }
        }
        return null;
    }

}
