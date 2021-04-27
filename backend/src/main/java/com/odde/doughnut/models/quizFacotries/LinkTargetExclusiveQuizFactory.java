package com.odde.doughnut.models.quizFacotries;

import com.odde.doughnut.entities.Link;
import com.odde.doughnut.entities.Note;
import com.odde.doughnut.entities.ReviewPoint;
import com.odde.doughnut.entities.QuizQuestion;

import java.util.List;

public class LinkTargetExclusiveQuizFactory implements QuizQuestionFactory {
    private final Link link;
    private final QuizQuestionServant servant;

    public LinkTargetExclusiveQuizFactory(QuizQuestionServant servant, ReviewPoint reviewPoint) {
        this.link = reviewPoint.getLink();
        this.servant = servant;
    }

    @Override
    public List<Note> generateFillingOptions() {
        Note sourceNote = link.getSourceNote();
        List<Note> backwardPeers = link.getBackwardPeers();
        return servant.randomlyChooseAndEnsure(backwardPeers, sourceNote, 5);
    }

    @Override
    public String generateInstruction() {
        return String.format("Which of the following %s", link.getExclusiveQuestion());
    }

    @Override
    public String generateMainTopic() {
        return link.getTargetNote().getTitle();
    }

    @Override
    public Note generateAnswerNote() {
        Note note = link.getSourceNote();
        List<Note> siblings = note.getSiblings();
        siblings.removeAll(link.getBackwardPeers());
        siblings.remove(link.getTargetNote());
        return servant.randomizer.chooseOneRandomly(siblings);
    }

    @Override
    public List<QuizQuestion.Option> toQuestionOptions(List<Note> notes) {
        return servant.toTitleOptions(notes);
    }
}