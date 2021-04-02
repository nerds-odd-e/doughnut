package com.odde.doughnut.models;

import com.odde.doughnut.entities.LinkEntity;
import com.odde.doughnut.entities.NoteEntity;
import com.odde.doughnut.entities.SubscriptionEntity;
import com.odde.doughnut.services.ModelFactoryService;

import java.util.ArrayList;
import java.util.List;

public class SubscriptionModel extends ModelForEntity<SubscriptionEntity> implements ReviewScope {
    public SubscriptionModel(SubscriptionEntity sub, ModelFactoryService modelFactoryService) {
        super(sub, modelFactoryService);
    }

    @Override
    public List<NoteEntity> getNotesHaveNotBeenReviewedAtAll() {
        return modelFactoryService.noteRepository.findByAncestorWhereThereIsNoReviewPoint(entity.getUserEntity().getId(), entity.getNoteEntity().getId());
    }

    @Override
    public int getNotesHaveNotBeenReviewedAtAllCount() {
        return 0;
    }

    @Override
    public List<LinkEntity> getLinksHaveNotBeenReviewedAtAll() {
        return new ArrayList<>();
    }

    public boolean needToLearnMoreToday(List<Integer> noteIds) {
        int count = modelFactoryService.noteRepository.countByAncestorAndInTheList(entity.getNoteEntity().getId(), noteIds);
        return count < entity.getDailyTargetOfNewNotes();
    }
}
