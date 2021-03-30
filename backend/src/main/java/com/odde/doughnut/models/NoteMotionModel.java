package com.odde.doughnut.models;

import com.odde.doughnut.entities.NoteEntity;
import com.odde.doughnut.entities.NoteMotionEntity;
import com.odde.doughnut.exceptions.CyclicLinkDetectedException;
import com.odde.doughnut.services.ModelFactoryService;

import java.util.ArrayList;

public class NoteMotionModel extends ModelForEntity<NoteMotionEntity>{
    private final NoteEntity subject;

    public NoteMotionModel(NoteMotionEntity noteMotionEntity, NoteEntity subject, ModelFactoryService modelFactoryService) {
        super(noteMotionEntity, modelFactoryService);
        this.subject = subject;
    }

    public void execute() throws CyclicLinkDetectedException {
        TreeNodeModel treeNodeModel = modelFactoryService.toTreeNodeModel(entity.getRelativeToNote());

        if(treeNodeModel.getAncestorsIncludingMe().contains(subject)) {
            throw new CyclicLinkDetectedException();
        }
        updateAncestors(subject, this.entity.getNewParent());
        Long newSiblingOrder = treeNodeModel.theSiblingOrderItTakesToMoveRelativeToMe(entity.isAsFirstChildOfNote());
        if (newSiblingOrder != null) {
            this.subject.setSiblingOrder(newSiblingOrder);
        }
        modelFactoryService.noteRepository.save(subject);
        subject.traverseBreathFirst(desc-> {
            updateAncestors(desc, desc.getParentNote());
            modelFactoryService.noteRepository.save(desc);
        });
    }

    private void updateAncestors(NoteEntity note, NoteEntity parent) {
        note.getNotesClosures().forEach(modelFactoryService.notesClosureRepository::delete);
        note.setNotesClosures(new ArrayList<>());
        modelFactoryService.entityManager.flush();
        note.setParentNote(parent);
    }

}
