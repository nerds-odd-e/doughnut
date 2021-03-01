package com.odde.doughnut.models;

import com.odde.doughnut.entities.Note;
import com.odde.doughnut.entities.repositories.NoteRepository;
import com.odde.doughnut.services.ModelFactoryService;
import org.apache.logging.log4j.util.Strings;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

public class NoteModel {
    private final NoteRepository noteRepository;
    private final Note note;

    public NoteModel(Note note, ModelFactoryService modelFactoryService) {
        this.noteRepository = modelFactoryService.noteRepository;
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

    public void linkNote(Note targetNote) {
        note.linkToNote(targetNote);
        note.setUpdatedDatetime(new Date());
        noteRepository.save(note);
    }


    private Note nextSiblingOfNote(Note note) {
        if (note.getParentNote() == null) {
            return null;
        }
        return noteRepository.findFirstByParentNoteAndSiblingOrderGreaterThanOrderBySiblingOrder(note.getParentNote(), note.getSiblingOrder());
    }

}
