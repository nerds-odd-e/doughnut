package com.odde.doughnut.models;

import com.odde.doughnut.entities.Note;
import com.odde.doughnut.entities.NoteMotionEntity;
import com.odde.doughnut.exceptions.CyclicLinkDetectedException;
import com.odde.doughnut.services.ModelFactoryService;

import java.util.ArrayList;

public class NoteMotionModel extends ModelForEntity<NoteMotionEntity>{

    public NoteMotionModel(NoteMotionEntity noteMotionEntity, ModelFactoryService modelFactoryService) {
        super(noteMotionEntity, modelFactoryService);
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
