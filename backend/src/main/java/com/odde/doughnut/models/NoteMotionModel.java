package com.odde.doughnut.models;

import com.odde.doughnut.entities.Note;
import com.odde.doughnut.entities.NoteMotion;
import com.odde.doughnut.exceptions.CyclicLinkDetectedException;
import com.odde.doughnut.services.ModelFactoryService;

import java.util.ArrayList;

public class NoteMotionModel extends ModelForEntity<NoteMotion>{

    public NoteMotionModel(NoteMotion noteMotion, ModelFactoryService modelFactoryService) {
        super(noteMotion, modelFactoryService);
    }

    public void execute() throws CyclicLinkDetectedException {
        entity.moveHeadNoteOnly();
        updateAncestors(entity.getSubject(), entity.getNewParent());
        modelFactoryService.noteRepository.save(entity.getSubject());
        entity.getSubject().traverseBreadthFirst(desc-> {
            updateAncestors(desc, desc.getParentNote());
            modelFactoryService.noteRepository.save(desc);
        });
    }

    private void updateAncestors(Note note, Note parent) {
        note.getNotesClosures().forEach(modelFactoryService.notesClosureRepository::delete);
        note.setNotesClosures(new ArrayList<>());
        modelFactoryService.entityManager.flush();
        note.setParentNote(parent);
    }
}
