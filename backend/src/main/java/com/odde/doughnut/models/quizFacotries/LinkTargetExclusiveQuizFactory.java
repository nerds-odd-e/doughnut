package com.odde.doughnut.models.quizFacotries;

import com.odde.doughnut.entities.LinkEntity;
import com.odde.doughnut.entities.Note;
import com.odde.doughnut.entities.ReviewPointEntity;
import com.odde.doughnut.models.QuizQuestion;

import java.util.List;

public class LinkTargetExclusiveQuizFactory implements QuizQuestionFactory {
    private final LinkEntity linkEntity;
    private final QuizQuestionServant servant;

    public LinkTargetExclusiveQuizFactory(QuizQuestionServant servant, ReviewPointEntity reviewPointEntity) {
        this.linkEntity = reviewPointEntity.getLinkEntity();
        this.servant = servant;
    }

    @Override
    public List<Note> generateFillingOptions() {
        Note sourceNote = linkEntity.getSourceNote();
        List<Note> backwardPeers = linkEntity.getBackwardPeers();
        return servant.randomlyChooseAndEnsure(backwardPeers, sourceNote, 5);
    }

    @Override
    public String generateInstruction() {
        return String.format("Which of the following %s", linkEntity.getExclusiveQuestion());
    }

    @Override
    public String generateMainTopic() {
        return linkEntity.getTargetNote().getTitle();
    }

    @Override
    public Note generateAnswerNote() {
        Note note = linkEntity.getSourceNote();
        List<Note> siblings = note.getSiblings();
        siblings.removeAll(linkEntity.getBackwardPeers());
        siblings.remove(linkEntity.getTargetNote());
        return servant.randomizer.chooseOneRandomly(siblings);
    }

    @Override
    public List<QuizQuestion.Option> toQuestionOptions(List<Note> notes) {
        return servant.toTitleOptions(notes);
    }
}