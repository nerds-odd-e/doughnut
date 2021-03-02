package com.odde.doughnut.models;

import com.odde.doughnut.entities.NoteEntity;
import com.odde.doughnut.entities.repositories.NoteRepository;
import com.odde.doughnut.services.ModelFactoryService;
import org.apache.logging.log4j.util.Strings;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

public class NoteModel {
    private final NoteRepository noteRepository;
    private final NoteEntity note;

    public NoteModel(NoteEntity note, ModelFactoryService modelFactoryService) {
        this.noteRepository = modelFactoryService.noteRepository;
        this.note = note;
    }

    public List<NoteEntity> getAncestors() {
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

    public NoteEntity getPreviousSiblingNote() {
        if(note == null || note.getParentNote() == null) {
            return null;
        }
        return noteRepository.findFirstByParentNoteAndSiblingOrderLessThanOrderBySiblingOrderDesc(note.getParentNote(), note.getSiblingOrder());
    }

    public NoteEntity getNextSiblingNote() {
        return nextSiblingOfNote(note);
    }

    public NoteEntity getPreviousNote() {
        NoteEntity result = getPreviousSiblingNote();
        if (result == null) {
            return note.getParentNote();
        }
        while(true) {
            NoteEntity lastChild =noteRepository.findFirstByParentNoteOrderBySiblingOrderDesc(result);
            if (lastChild == null) {
                return result;
            }
            result = lastChild;
        }
    }

    public NoteEntity getNextNote() {
        NoteEntity firstChild = getFirstChild();
        if (firstChild != null) {
            return firstChild;
        }
        NoteEntity next = note;
        while(next != null) {
            NoteEntity sibling = nextSiblingOfNote(next);
            if (sibling != null) {
                return sibling;
            }
            next = next.getParentNote();
        }
        return null;
    }

    public void linkNote(NoteEntity targetNote) {
        note.linkToNote(targetNote);
        note.setUpdatedDatetime(new Date());
        noteRepository.save(note);
    }


    private NoteEntity nextSiblingOfNote(NoteEntity note) {
        if (note.getParentNote() == null) {
            return null;
        }
        return noteRepository.findFirstByParentNoteAndSiblingOrderGreaterThanOrderBySiblingOrder(note.getParentNote(), note.getSiblingOrder());
    }

    public NoteEntity getFirstChild() {
        return noteRepository.findFirstByParentNoteOrderBySiblingOrder(note);
    }

    private long getSiblingOrderToInsertBehindMe() {
        NoteEntity nextSiblingNote = getNextSiblingNote();
        Long relativeToSiblingOrder = note.getSiblingOrder();
        if (nextSiblingNote == null) {
            return relativeToSiblingOrder + NoteEntity.MINIMUM_SIBLING_ORDER_INCREMENT;
        }
        return (relativeToSiblingOrder + nextSiblingNote.getSiblingOrder()) / 2;
    }

    private Long getSiblingOrderToBecomeMyFirstChild() {
        NoteEntity firstChild = getFirstChild();
        if (firstChild != null) {
            return firstChild.getSiblingOrder() - NoteEntity.MINIMUM_SIBLING_ORDER_INCREMENT;
        }
        return null;
    }

    Long theSiblingOrderItTakesToMoveRelativeToMe(boolean asFirstChildOfNote) {
        if (!asFirstChildOfNote) {
            return getSiblingOrderToInsertBehindMe();
        }
        return getSiblingOrderToBecomeMyFirstChild();
    }
}
