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
        this.answerNote = getAnswerNote();
        this.user = reviewPoint.getUser();
    }

    @Override
    public List<Note> generateFillingOptions(QuizQuestionServant servant) {
        if(cachedFillingOptions == null) {
            List<Note> uncles = link.getPiblingOfTheSameLinkType(user);
            cachedFillingOptions = servant.choose5FromSiblings(answerNote, n -> !n.equals(answerNote) && !n.equals(link.getSourceNote()) && !uncles.contains(n));
        }
        return cachedFillingOptions;
    }

    @Override
    public String generateInstruction() {
        return "<mark>" + link.getSourceNote().getTitle() + "</mark> " + link.getLinkTypeLabel() + ":";
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
    public Map<Link.LinkType, LinkViewed> generateHintLinks() {
        return null;
    }

    @Override
    public int minimumFillingOptionCount() {
        return 1;
    }

    private Note getAnswerNote() {
        return link.getTargetNote();
    }

}