package com.odde.doughnut.models.quizFacotries;

import com.odde.doughnut.entities.Link;
import com.odde.doughnut.entities.Note;
import com.odde.doughnut.entities.ReviewPoint;

import java.util.List;

public class LinkTargetExclusiveQuizFactory implements QuizQuestionFactory {
    private final Link link;
    private final ReviewPoint reviewPoint;
    private List<Note> cachedFillingOptions = null;
    private Note answerNote = null;

    public LinkTargetExclusiveQuizFactory(ReviewPoint reviewPoint) {
        this.reviewPoint = reviewPoint;
        this.link = reviewPoint.getLink();
    }

    @Override
    public List<Note> generateFillingOptions(QuizQuestionServant servant) {
        if(cachedFillingOptions == null) {
            Note sourceNote = link.getSourceNote();
            List<Note> backwardPeers = link.getCousinOfSameLinkType(reviewPoint.getUser());
            cachedFillingOptions = servant.randomlyChooseAndEnsure(backwardPeers, sourceNote, 5);
        }
        return cachedFillingOptions;
    }

    @Override
    public String generateInstruction() {
        return String.format("Which of the following is <em>NOT</em> %s", link.getLinkType().label);
    }

    @Override
    public Note generateAnswerNote(QuizQuestionServant servant) {
        if (answerNote == null) {
            Note note = link.getSourceNote();
            List<Note> siblings = note.getSiblings();
            siblings.removeAll(link.getCousinOfSameLinkType(reviewPoint.getUser()));
            siblings.remove(link.getTargetNote());
            siblings.remove(link.getSourceNote());
            answerNote = servant.randomizer.chooseOneRandomly(siblings);
        }
        return answerNote;
    }

    @Override
    public int minimumFillingOptionCount() {
        return 1;
    }

    @Override
    public List<Note> allWrongAnswers() {
        return List.of(link.getSourceNote());
    }

}