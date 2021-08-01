package com.odde.doughnut.models.quizFacotries;

import com.odde.doughnut.entities.*;
import com.odde.doughnut.entities.json.LinkViewed;

import java.util.List;
import java.util.Map;

public class LinkTargetQuizFactory implements QuizQuestionFactory {
    protected final Link link;
    protected final Note answerNote;
    protected final QuizQuestionServant servant;
    private final User user;
    private List<Note> cachedFillingOptions = null;

    public LinkTargetQuizFactory(QuizQuestionServant servant, ReviewPoint reviewPoint) {
        this.link = reviewPoint.getLink();
        this.servant = servant;
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
    public Note generateAnswerNote() {
        return answerNote;
    }

    @Override
    public List<QuizQuestion.Option> toQuestionOptions(QuizQuestionServant servant, List<Note> noteEntities) {
        return servant.toTitleOptions(noteEntities);
    }

    @Override
    public Map<Link.LinkType, LinkViewed> generateHintLinks() {
        return null;
    }

    @Override
    public boolean isValidQuestion() {
        return generateFillingOptions(this.servant).size() > 0;
    }

    private Note getAnswerNote() {
        return link.getTargetNote();
    }

}