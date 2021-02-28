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
        return note.getChildren().stream().findFirst().orElse(getNextSiblingNote());
    }

    public Note getNextSiblingNote() {
        if (note.getParentNote() == null) {
            return null;
        }
        return noteRepository.findFirstByParentNoteAndSiblingOrderGreaterThanOrderBySiblingOrder(note.getParentNote(), note.getSiblingOrder());
    }

    public Note getPreviousNote() {
        Note result = getPreviousSiblingNote();
        if (result != null) {
            return result;
        }
        return note.getParentNote();
    }

    public Note getPreviousSiblingNote() {
        if (note.getParentNote() == null) {
            return null;
        }
        return noteRepository.findFirstByParentNoteAndSiblingOrderLessThanOrderBySiblingOrderDesc(note.getParentNote(), note.getSiblingOrder());
    }

}
