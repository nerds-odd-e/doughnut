package com.odde.doughnut.models.quizFacotries;

import com.odde.doughnut.entities.*;
import com.odde.doughnut.entities.json.LinkViewed;

import java.util.List;
import java.util.Map;

public class LinkSourceQuizFactory implements QuizQuestionFactory {
    protected final Link link;
    protected final Note answerNote;
    protected final QuizQuestionServant servant;
    private final User user;
    private List<Note> cachedFillingOptions = null;

    public LinkSourceQuizFactory(QuizQuestionServant servant, ReviewPoint reviewPoint) {
        this.link = reviewPoint.getLink();
        this.servant = servant;
        this.answerNote = link.getSourceNote();
        this.user = reviewPoint.getUser();
    }

    @Override
    public List<Note> generateFillingOptions(QuizQuestionServant servant) {
        if(cachedFillingOptions == null) {
            List<Note> cousinOfSameLinkType = link.getCousinOfSameLinkType(user);
            cachedFillingOptions = servant.choose5FromSiblings(answerNote, n -> !n.equals(answerNote) && !n.equals(link.getTargetNote()) && !cousinOfSameLinkType.contains(n));
        }
        return cachedFillingOptions;
    }

    @Override
    public String generateInstruction() {
        return "Which one <em>" + link.getLinkTypeLabel() + "</em>:";
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
    public boolean isValidQuestion() {
        return generateFillingOptions(this.servant).size() > 0;
    }

}