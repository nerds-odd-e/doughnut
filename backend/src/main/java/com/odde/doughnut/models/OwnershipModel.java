package com.odde.doughnut.models;

import com.odde.doughnut.entities.NoteEntity;
import com.odde.doughnut.entities.OwnershipEntity;
import com.odde.doughnut.services.ModelFactoryService;

import java.util.List;

public class OwnershipModel extends ModelForEntity<OwnershipEntity>{
    public OwnershipModel(OwnershipEntity ownershipEntity, ModelFactoryService modelFactoryService) {
        super(ownershipEntity, modelFactoryService);
    }

    public List<NoteEntity> getOrphanedNotes() {
        return modelFactoryService.noteRepository.orphanedNotesOf(entity);
    }
}
