package com.odde.doughnut.models.quizFacotries;

import com.odde.doughnut.entities.Link;
import com.odde.doughnut.entities.Note;
import com.odde.doughnut.entities.ReviewPoint;
import com.odde.doughnut.entities.QuizQuestion;
import com.odde.doughnut.entities.json.LinkViewed;

import java.util.List;
import java.util.Map;

public class LinkTargetQuizFactory implements QuizQuestionFactory {
    private final Link link;
    private final Note answerNote;
    private final QuizQuestionServant servant;
    private List<Note> cachedFillingOptions = null;

    public LinkTargetQuizFactory(QuizQuestionServant servant, ReviewPoint reviewPoint) {
        this.link = reviewPoint.getLink();
        this.servant = servant;
        this.answerNote = getAnswerNote();
    }

    @Override
    public List<Note> generateFillingOptions() {
        if(cachedFillingOptions == null) {
            cachedFillingOptions = servant.choose5FromSiblings(answerNote, n -> !n.equals(answerNote) && !n.equals(link.getSourceNote()));
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
    public List<QuizQuestion.Option> toQuestionOptions(List<Note> noteEntities) {
        return servant.toTitleOptions(noteEntities);
    }

    @Override
    public Map<Link.LinkType, LinkViewed> generateHintLinks() {
        return null;
    }

    @Override
    public boolean isValidQuestion() {
        return generateFillingOptions().size() > 0;
    }

    private Note getAnswerNote() {
        return link.getTargetNote();
    }

}