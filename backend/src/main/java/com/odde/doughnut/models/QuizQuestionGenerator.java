package com.odde.doughnut.models;

import com.odde.doughnut.entities.NoteEntity;
import com.odde.doughnut.entities.ReviewPointEntity;
import com.odde.doughnut.entities.ReviewSettingEntity;
import com.odde.doughnut.services.ModelFactoryService;
import org.apache.logging.log4j.util.Strings;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class QuizQuestionGenerator {
    private final ReviewPointEntity reviewPointEntity;
    private final Randomizer randomizer;
    private final ModelFactoryService modelFactoryService;

    public QuizQuestionGenerator(ReviewPointEntity reviewPointEntity, Randomizer randomizer, ModelFactoryService modelFactoryService) {
        this.reviewPointEntity = reviewPointEntity;
        this.randomizer = randomizer;
        this.modelFactoryService = modelFactoryService;
    }

    public QuizQuestion.QuestionType generateQuestionType() {
        List<QuizQuestion.QuestionType> questionTypes = availableQuestionTypes();
        return randomizer.chooseOneRandomly(questionTypes);
    }

    List<QuizQuestion.QuestionType> availableQuestionTypes() {
        List<QuizQuestion.QuestionType> questionTypes = new ArrayList<>();
        if (reviewPointEntity.getLinkEntity() != null) {
            questionTypes.add(QuizQuestion.QuestionType.LINK_TARGET);
            questionTypes.add(QuizQuestion.QuestionType.LINK_SOURCE_EXCLUSIVE);
        }
        else {
            NoteEntity noteEntity = reviewPointEntity.getNoteEntity();
            if (!Strings.isEmpty(noteEntity.getDescription())) {
                ReviewSettingEntity reviewSettingEntity = noteEntity.getMasterReviewSettingEntity();
                if (reviewSettingEntity != null && reviewSettingEntity.getRememberSpelling()) {
                    questionTypes.add(QuizQuestion.QuestionType.SPELLING);
                }
                questionTypes.add(QuizQuestion.QuestionType.CLOZE_SELECTION);
            }
            if (!Strings.isEmpty(noteEntity.getNotePicture())) {
                questionTypes.add(QuizQuestion.QuestionType.PICTURE_TITLE);
                questionTypes.add(QuizQuestion.QuestionType.PICTURE_SELECTION);
            }
        }
        return questionTypes;
    }

    QuizQuestion generateQuestion(Randomizer randomizer, ReviewPointEntity entity) {
        QuizQuestion.QuestionType questionType = generateQuestionType();
        return generateQuestionOfType(randomizer, entity, questionType);
    }

    QuizQuestion generateQuestionOfType(Randomizer randomizer, ReviewPointEntity entity, QuizQuestion.QuestionType questionType) {
        QuizQuestion quizQuestion = new QuizQuestion(entity, randomizer, modelFactoryService);
        if (questionType == null) {
            return null;
        }
        quizQuestion.setQuestionType(questionType);
        quizQuestion.setOptions(generateOptions(questionType));

        return quizQuestion;
    }

    public List<QuizQuestion.Option> generateOptions(QuizQuestion.QuestionType questionType) {
        TreeNodeModel treeNodeModel = getAnswerTreeNodeModel(questionType);
        Stream<NoteEntity> noteEntityStream = treeNodeModel.getSiblings().stream()
                .filter(t -> !t.getTitle().equals(getAnswerNote(questionType).getTitle()));
        if(questionType == QuizQuestion.QuestionType.PICTURE_SELECTION) {
            noteEntityStream = noteEntityStream.filter(n -> Strings.isNotEmpty(n.getNotePicture()));
        }
        List<NoteEntity> list = noteEntityStream.collect(Collectors.toList());
        randomizer.shuffle(list);
        List<NoteEntity> selectedList = list.stream().limit(5).collect(Collectors.toList());
        selectedList.add(getAnswerNote(questionType));
        randomizer.shuffle(selectedList);

        if (questionType == QuizQuestion.QuestionType.PICTURE_SELECTION) {
            return toPictureOptions(selectedList);
        }

        return toTitleOptions(selectedList);
    }

    private List<QuizQuestion.Option> toPictureOptions(List<NoteEntity> selectedList) {
        return selectedList.stream().map(QuizQuestion.Option::createPictureOption).collect(Collectors.toUnmodifiableList());
    }

    private List<QuizQuestion.Option> toTitleOptions(List<NoteEntity> selectedList) {
        return selectedList.stream().map(QuizQuestion.Option::createTitleOption).collect(Collectors.toUnmodifiableList());
    }

    private TreeNodeModel getAnswerTreeNodeModel(QuizQuestion.QuestionType questionType) {
        return modelFactoryService.toTreeNodeModel(getAnswerNote(questionType));
    }

    private NoteEntity getAnswerNote(QuizQuestion.QuestionType questionType) {
        if (questionType == QuizQuestion.QuestionType.LINK_TARGET) {
            return reviewPointEntity.getLinkEntity().getTargetNote();
        }
        return reviewPointEntity.getNoteEntity();
    }

}
