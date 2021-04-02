package com.odde.doughnut.models;

import com.odde.doughnut.entities.LinkEntity;
import com.odde.doughnut.entities.NoteEntity;
import com.odde.doughnut.entities.SubscriptionEntity;
import com.odde.doughnut.services.ModelFactoryService;

import java.util.List;

public class SubscriptionModel extends ModelForEntity<SubscriptionEntity> implements ReviewScope {
    public SubscriptionModel(SubscriptionEntity sub, ModelFactoryService modelFactoryService) {
        super(sub, modelFactoryService);
    }

    @Override
    public List<NoteEntity> getNotesHaveNotBeenReviewedAtAll() {
        return modelFactoryService.noteRepository.findByAncestorWhereThereIsNoReviewPoint(entity.getUserEntity(), entity.getNoteEntity());
    }

    @Override
    public int getNotesHaveNotBeenReviewedAtAllCount() {
        return modelFactoryService.noteRepository.countByAncestorWhereThereIsNoReviewPoint(entity.getUserEntity(), entity.getNoteEntity());
    }

    @Override
    public List<LinkEntity> getLinksHaveNotBeenReviewedAtAll() {
        return modelFactoryService.linkRepository.findByAncestorWhereThereIsNoReviewPoint(entity.getUserEntity(), entity.getNoteEntity());
    }

    public boolean needToLearnMoreToday(List<Integer> noteIds) {
        int count = modelFactoryService.noteRepository.countByAncestorAndInTheList(entity.getNoteEntity(), noteIds);
        return count < entity.getDailyTargetOfNewNotes();
    }
}
