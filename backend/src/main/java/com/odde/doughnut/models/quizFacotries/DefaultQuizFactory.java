package com.odde.doughnut.models.quizFacotries;

import com.odde.doughnut.entities.NoteEntity;
import com.odde.doughnut.entities.ReviewPointEntity;
import com.odde.doughnut.models.QuizQuestion;
import com.odde.doughnut.models.Randomizer;
import com.odde.doughnut.models.TreeNodeModel;
import com.odde.doughnut.services.ModelFactoryService;
import lombok.Setter;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static com.odde.doughnut.models.QuizQuestion.QuestionType.LINK_TARGET;
import static com.odde.doughnut.models.QuizQuestion.QuestionType.PICTURE_SELECTION;

public class DefaultQuizFactory implements QuizQuestionFactory {
    private final ReviewPointEntity reviewPointEntity;
    private NoteEntity answerNote;
    private final Randomizer randomizer;
    private final ModelFactoryService modelFactoryService;
    private QuizQuestion.QuestionType questionType;
    private final QuizQuestionServant quizQuestionServant = new QuizQuestionServant();

    public DefaultQuizFactory(ReviewPointEntity reviewPointEntity, Randomizer randomizer, ModelFactoryService modelFactoryService) {
        this.reviewPointEntity = reviewPointEntity;
        this.randomizer = randomizer;
        this.modelFactoryService = modelFactoryService;
    }

    public void setQuestionType(QuizQuestion.QuestionType questionType) {
        this.questionType = questionType;
        this.answerNote = getAnswerNote();
    }

    @Override
    public List<NoteEntity> generateFillingOptions() {
        List<NoteEntity> selectedList;
        Stream<NoteEntity> noteEntityStream;
        noteEntityStream = getAnswerTreeNodeModel().getSiblings().stream()
                .filter(n -> !n.equals(answerNote));
        if (questionType == PICTURE_SELECTION) {
            noteEntityStream = noteEntityStream.filter(NoteEntity::hasPicture);
        }
        List<NoteEntity> list = noteEntityStream.collect(Collectors.toList());
        selectedList = randomizer.randomlyChoose(5, list);
        return selectedList;
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
            return quizQuestionServant.toPictureOptions(noteEntities);
        }

        return quizQuestionServant.toTitleOptions(noteEntities);
    }

    private TreeNodeModel getAnswerTreeNodeModel() {
        return modelFactoryService.toTreeNodeModel(answerNote);
    }

    private NoteEntity getAnswerNote() {
        if (questionType == LINK_TARGET) {
            return reviewPointEntity.getLinkEntity().getTargetNote();
        }
        return reviewPointEntity.getNoteEntity();
    }

}