package com.odde.doughnut.models;

import com.odde.doughnut.entities.NoteEntity;
import com.odde.doughnut.entities.NoteMotionEntity;
import com.odde.doughnut.exceptions.CyclicLinkDetectedException;
import com.odde.doughnut.services.ModelFactoryService;

public class NoteMotionModel extends ModelForEntity<NoteMotionEntity>{
    private final NoteEntity subject;

    public NoteMotionModel(NoteMotionEntity noteMotionEntity, NoteEntity subject, ModelFactoryService modelFactoryService) {
        super(noteMotionEntity, modelFactoryService);
        this.subject = subject;
    }

    public void execute() throws CyclicLinkDetectedException {
        TreeNodeModel treeNodeModel = modelFactoryService.toTreeNodeModel(entity.getRelativeToNote());

        if(treeNodeModel.getAncestors().contains(subject)) {
            throw new CyclicLinkDetectedException();
        }
        subject.setParentNote(entity.getNewParent());
        Long newSiblingOrder = treeNodeModel.theSiblingOrderItTakesToMoveRelativeToMe(entity.isAsFirstChildOfNote());
        if (newSiblingOrder != null) {
            subject.setSiblingOrder(newSiblingOrder);
        }
        modelFactoryService.noteRepository.save(subject);
    }

}
