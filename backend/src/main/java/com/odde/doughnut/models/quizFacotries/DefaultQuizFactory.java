package com.odde.doughnut.models.quizFacotries;

import com.odde.doughnut.entities.NoteEntity;
import com.odde.doughnut.entities.ReviewPointEntity;
import com.odde.doughnut.models.QuizQuestion;
import com.odde.doughnut.models.Randomizer;
import com.odde.doughnut.services.ModelFactoryService;

import java.util.List;

import static com.odde.doughnut.models.QuizQuestion.QuestionType.LINK_TARGET;
import static com.odde.doughnut.models.QuizQuestion.QuestionType.PICTURE_SELECTION;

public class DefaultQuizFactory implements QuizQuestionFactory {
    private final ReviewPointEntity reviewPointEntity;
    private NoteEntity answerNote;
    private final Randomizer randomizer;
    private QuizQuestion.QuestionType questionType;
    private final QuizQuestionServant servant;

    public DefaultQuizFactory(ReviewPointEntity reviewPointEntity, Randomizer randomizer, ModelFactoryService modelFactoryService) {
        this.reviewPointEntity = reviewPointEntity;
        this.randomizer = randomizer;
        servant = new QuizQuestionServant(randomizer, modelFactoryService);
    }

    public void setQuestionType(QuizQuestion.QuestionType questionType) {
        this.questionType = questionType;
        this.answerNote = getAnswerNote();
    }

    @Override
    public List<NoteEntity> generateFillingOptions() {
        return servant.choose5FromSiblings(answerNote, randomizer, n -> {
            if (questionType == PICTURE_SELECTION) {
                if (!n.hasPicture()) return false;
            }
            return !n.equals(answerNote);
        });
    }

    @Override
    public String generateInstruction() {
        if (questionType.equals(PICTURE_SELECTION)) {
            return "";
        }
        if (questionType.equals(LINK_TARGET)) {
            return reviewPointEntity.getLinkEntity().getQuizDescription();
        }
        return answerNote.getClozeDescription();
    }

    @Override
    public String generateMainTopic() {
        if (questionType.equals(PICTURE_SELECTION)) {
            return answerNote.getTitle();
        }
        return "";
    }

    @Override
    public NoteEntity generateAnswerNote() {
        return answerNote;
    }

    @Override
    public List<QuizQuestion.Option> toQuestionOptions(List<NoteEntity> noteEntities) {
        if (questionType == PICTURE_SELECTION) {
            return servant.toPictureOptions(noteEntities);
        }

        return servant.toTitleOptions(noteEntities);
    }

    private NoteEntity getAnswerNote() {
        if (questionType == LINK_TARGET) {
            return reviewPointEntity.getLinkEntity().getTargetNote();
        }
        return reviewPointEntity.getNoteEntity();
    }

}