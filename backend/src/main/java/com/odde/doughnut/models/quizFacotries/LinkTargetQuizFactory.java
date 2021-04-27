package com.odde.doughnut.models.quizFacotries;

import com.odde.doughnut.entities.Link;
import com.odde.doughnut.entities.Note;
import com.odde.doughnut.entities.ReviewPoint;
import com.odde.doughnut.entities.QuizQuestion;

import java.util.List;

public class LinkTargetQuizFactory implements QuizQuestionFactory {
    private final Link link;
    private final Note answerNote;
    private final QuizQuestionServant servant;

    public LinkTargetQuizFactory(QuizQuestionServant servant, ReviewPoint reviewPoint) {
        this.link = reviewPoint.getLink();
        this.servant = servant;
        this.answerNote = getAnswerNote();
    }

    @Override
    public List<Note> generateFillingOptions() {
        return servant.choose5FromSiblings(answerNote, n -> !n.equals(answerNote));
    }

    @Override
    public String generateInstruction() {
        return "<mark>" + link.getSourceNote().getTitle() + "</mark> " + link.getType() + ":";
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

    private Note getAnswerNote() {
        return link.getTargetNote();
    }

}