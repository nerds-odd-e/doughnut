package com.odde.doughnut.models.quizFacotries;

import com.odde.doughnut.entities.*;
import com.odde.doughnut.entities.json.LinkViewed;

import java.util.List;
import java.util.Map;

public class LinkSourceQuizFactory implements QuizQuestionFactory {
    protected final Link link;
    protected final Note answerNote;
    private final User user;
    private List<Note> cachedFillingOptions = null;

    public LinkSourceQuizFactory(ReviewPoint reviewPoint) {
        this.link = reviewPoint.getLink();
        this.answerNote = link.getSourceNote();
        this.user = reviewPoint.getUser();
    }

    @Override
    public List<Note> generateFillingOptions(QuizQuestionServant servant) {
        if(cachedFillingOptions == null) {
            List<Note> cousinOfSameLinkType = link.getCousinOfSameLinkType(user);
            cachedFillingOptions = servant.choose5FromCohort(answerNote, n -> !n.equals(answerNote) && !n.equals(link.getTargetNote()) && !cousinOfSameLinkType.contains(n));
        }
        return cachedFillingOptions;
    }

    @Override
    public String generateInstruction() {
        return "Which one <em>is " + link.getLinkTypeLabel() + "</em>:";
    }

    @Override
    public String generateMainTopic() {
        return link.getTargetNote().getTitle();
    }

    @Override
    public Note generateAnswerNote(QuizQuestionServant servant) {
        return answerNote;
    }

    @Override
    public Map<Link.LinkType, LinkViewed> generateHintLinks() {
        return null;
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