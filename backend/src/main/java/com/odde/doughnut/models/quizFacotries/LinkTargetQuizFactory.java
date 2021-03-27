package com.odde.doughnut.models.quizFacotries;

import com.odde.doughnut.entities.LinkEntity;
import com.odde.doughnut.entities.NoteEntity;
import com.odde.doughnut.entities.ReviewPointEntity;
import com.odde.doughnut.models.QuizQuestion;
import com.odde.doughnut.models.Randomizer;
import com.odde.doughnut.services.ModelFactoryService;

import java.util.List;

import static com.odde.doughnut.models.QuizQuestion.QuestionType.LINK_TARGET;

public class LinkTargetQuizFactory implements QuizQuestionFactory {
    private final LinkEntity linkEntity;
    private final NoteEntity answerNote;
    private final Randomizer randomizer;
    private final QuizQuestionServant servant;

    public LinkTargetQuizFactory(ReviewPointEntity reviewPointEntity, Randomizer randomizer, ModelFactoryService modelFactoryService) {
        this.linkEntity = reviewPointEntity.getLinkEntity();
        this.randomizer = randomizer;
        servant = new QuizQuestionServant(randomizer, modelFactoryService);
        this.answerNote = getAnswerNote();
    }

    @Override
    public List<NoteEntity> generateFillingOptions() {
        return servant.choose5FromSiblings(answerNote, randomizer, n -> !n.equals(answerNote));
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