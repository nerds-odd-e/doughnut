package com.odde.doughnut.models.questionTypes;

import com.odde.doughnut.entities.Note;
import com.odde.doughnut.entities.QuizQuestion;
import com.odde.doughnut.entities.ReviewPoint;
import com.odde.doughnut.models.UserModel;
import com.odde.doughnut.models.quizFacotries.QuizQuestionDirector;
import com.odde.doughnut.models.randomizers.NonRandomizer;
import com.odde.doughnut.testability.MakeMe;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

import static com.odde.doughnut.entities.QuizQuestion.QuestionType.CLOZE_LINK_TARGET;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(locations = {"classpath:repository.xml"})
@Transactional
class QuizQuestionTypeClozeLinkTargetTest {
    @Autowired
    MakeMe makeMe;
    UserModel userModel;
    NonRandomizer randomizer = new NonRandomizer();
    Note top;
    Note target;
    Note source;
    Note anotherSource;
    ReviewPoint reviewPoint;


    @BeforeEach
    void setup() {
        userModel = makeMe.aUser().toModelPlease();
        top = makeMe.aNote().byUser(userModel).please();
        target = makeMe.aNote("rome").under(top).please();
        source = makeMe.aNote("Rome is not built in a day").under(top).linkTo(target).please();
        reviewPoint = makeMe.aReviewPointFor(source.getLinks().get(0)).inMemoryPlease();
        anotherSource = makeMe.aNote("pompeii").under(top).please();
        makeMe.refresh(top);
    }

    @Nested
    class WhenThereAreMoreThanOneOptions {
        @Test
        void shouldIncludeRightAnswers() {
            QuizQuestion quizQuestion = buildQuestion();
            assertThat(quizQuestion.getDescription(), equalTo("<mark><mark title='Hidden text that is matching the answer'>[...]</mark> is not built in a day</mark> is a specialization of:"));
        }
    }

    private QuizQuestion buildQuestion() {
        QuizQuestionDirector builder = new QuizQuestionDirector(CLOZE_LINK_TARGET, randomizer, reviewPoint, makeMe.modelFactoryService);
        return builder.buildQuizQuestion();
    }

}

