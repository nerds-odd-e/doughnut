package com.odde.doughnut.models.quizFacotries;

import com.odde.doughnut.entities.LinkEntity;
import com.odde.doughnut.entities.NoteEntity;
import com.odde.doughnut.entities.ReviewPointEntity;
import com.odde.doughnut.models.QuizQuestion;

import java.util.List;

public class LinkTargetQuizFactory implements QuizQuestionFactory {
    private final LinkEntity linkEntity;
    private final NoteEntity answerNote;
    private final QuizQuestionServant servant;

    public LinkTargetQuizFactory(QuizQuestionServant servant, ReviewPointEntity reviewPointEntity) {
        this.linkEntity = reviewPointEntity.getLinkEntity();
        this.servant = servant;
        this.answerNote = getAnswerNote();
    }

    @Override
    public List<NoteEntity> generateFillingOptions() {
        return servant.choose5FromSiblings(answerNote, n -> !n.equals(answerNote));
    }

    @Override
    public String generateInstruction() {
        return linkEntity.getQuizDescription();
    }

    @Override
    public String generateMainTopic() {
        return "";
    }

    @Override
    public NoteEntity generateAnswerNote() {
        return answerNote;
    }

    @Override
    public List<QuizQuestion.Option> toQuestionOptions(List<NoteEntity> noteEntities) {
        return servant.toTitleOptions(noteEntities);
    }

    private NoteEntity getAnswerNote() {
        return linkEntity.getTargetNote();
    }

}