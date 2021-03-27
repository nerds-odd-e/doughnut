package com.odde.doughnut.models.quizFacotries;

import com.odde.doughnut.entities.LinkEntity;
import com.odde.doughnut.entities.NoteEntity;
import com.odde.doughnut.entities.ReviewPointEntity;
import com.odde.doughnut.models.Randomizer;
import com.odde.doughnut.services.ModelFactoryService;

import java.util.List;
import java.util.stream.Collectors;

public class LinkTargetExclusiveQuizFactory {
    private final LinkEntity linkEntity;
    private final Randomizer randomizer;
    private final ModelFactoryService modelFactoryService;

    public LinkTargetExclusiveQuizFactory(ReviewPointEntity reviewPointEntity, Randomizer randomizer, ModelFactoryService modelFactoryService) {
        this.linkEntity = reviewPointEntity.getLinkEntity();
        this.randomizer = randomizer;
        this.modelFactoryService = modelFactoryService;
    }

    public List<NoteEntity> generateFillingOptions() {
        NoteEntity sourceNote = linkEntity.getSourceNote();
        List<NoteEntity> list = linkEntity.getBackwardPeers().stream()
                .filter(n -> !n.equals(sourceNote)).collect(Collectors.toList());
        List<NoteEntity> selectedList = randomizer.randomlyChoose(4, list);
        selectedList.add(sourceNote);
        return selectedList;
    }

    public String generateInstruction() {
        return String.format("Which of the following %s", linkEntity.getExclusiveQuestion());
    }

    public String generateMainTopic() {
        return linkEntity.getTargetNote().getTitle();
    }

    public NoteEntity generateAnswerNote() {
        NoteEntity note = linkEntity.getSourceNote();
        List<NoteEntity> siblings = modelFactoryService.toTreeNodeModel(note).getSiblings();
        siblings.removeAll(linkEntity.getBackwardPeers());
        siblings.remove(linkEntity.getTargetNote());
        return randomizer.chooseOneRandomly(siblings);
    }
}