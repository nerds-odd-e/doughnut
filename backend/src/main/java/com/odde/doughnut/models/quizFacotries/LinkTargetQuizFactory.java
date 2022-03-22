package com.odde.doughnut.models.quizFacotries;

import com.odde.doughnut.entities.*;
import com.odde.doughnut.entities.json.LinkViewed;

import java.util.List;
import java.util.Map;

public class LinkTargetQuizFactory implements QuizQuestionFactory {
    protected final Link link;
    protected final Note answerNote;
    private final User user;
    private List<Note> cachedFillingOptions = null;

    public LinkTargetQuizFactory(ReviewPoint reviewPoint) {
        this.link = reviewPoint.getLink();
        this.answerNote = link.getTargetNote();
        this.user = reviewPoint.getUser();
    }

    @Override
    public List<Note> generateFillingOptions(QuizQuestionServant servant) {
        if(cachedFillingOptions == null) {
            List<Note> uncles = link.getPiblingOfTheSameLinkType(user);
            cachedFillingOptions = servant.choose5FromCohort(answerNote, n -> !n.equals(answerNote) && !n.equals(link.getSourceNote()) && !uncles.contains(n));
        }
        return cachedFillingOptions;
    }

    @Override
    public String generateInstruction() {
        return "<mark>" + link.getSourceNote().getTitle() + "</mark> is " + link.getLinkTypeLabel() + ":";
    }

    @Override
    public String generateMainTopic() {
        return "";
    }

    @Override
    public Note generateAnswerNote(QuizQuestionServant servant) {
        return answerNote;
    }

    @Override
    public int minimumFillingOptionCount() {
        return 1;
    }

    @Override
    public List<Note> knownRightAnswers() {
        return List.of(answerNote);
    }

}