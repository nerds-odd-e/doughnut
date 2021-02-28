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

    public Note getPreviousSiblingNote() {
        if (note.getParentNote() == null) {
            return null;
        }
        return noteRepository.findFirstByParentNoteAndSiblingOrderLessThanOrderBySiblingOrderDesc(note.getParentNote(), note.getSiblingOrder());
    }

    public Note getNextSiblingNote() {
        return nextSiblingOfNote(note);
    }

    public Note getPreviousNote() {
        Note result = getPreviousSiblingNote();
        if (result == null) {
            return note.getParentNote();
        }
        while(true) {
            Note lastChild =noteRepository.findFirstByParentNoteOrderBySiblingOrderDesc(result);
            if (lastChild == null) {
                return result;
            }
            result = lastChild;
        }
    }

    public Note getNextNote() {
        Note firstChild = noteRepository.findFirstByParentNoteOrderBySiblingOrder(note);
        if (firstChild != null) {
            return firstChild;
        }
        Note next = note;
        while(next != null) {
            Note sibling = nextSiblingOfNote(next);
            if (sibling != null) {
                return sibling;
            }
            next = next.getParentNote();
        }
        return null;
    }

    private Note nextSiblingOfNote(Note note) {
        if (note.getParentNote() == null) {
            return null;
        }
        return noteRepository.findFirstByParentNoteAndSiblingOrderGreaterThanOrderBySiblingOrder(note.getParentNote(), note.getSiblingOrder());
    }

}
