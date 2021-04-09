package com.odde.doughnut.models.quizFacotries;

import com.odde.doughnut.entities.LinkEntity;
import com.odde.doughnut.entities.Note;
import com.odde.doughnut.entities.ReviewPointEntity;
import com.odde.doughnut.models.QuizQuestion;

import java.util.List;

public class LinkTargetQuizFactory implements QuizQuestionFactory {
    private final LinkEntity linkEntity;
    private final Note answerNote;
    private final QuizQuestionServant servant;

    public LinkTargetQuizFactory(QuizQuestionServant servant, ReviewPointEntity reviewPointEntity) {
        this.linkEntity = reviewPointEntity.getLinkEntity();
        this.servant = servant;
        this.answerNote = getAnswerNote();
    }

    @Override
    public List<Note> generateFillingOptions() {
        return servant.choose5FromSiblings(answerNote, n -> !n.equals(answerNote));
    }

    @Override
    public String generateInstruction() {
        return "<mark>" + linkEntity.getSourceNote().getTitle() + "</mark> " + linkEntity.getType() + ":";
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
        return linkEntity.getTargetNote();
    }

}