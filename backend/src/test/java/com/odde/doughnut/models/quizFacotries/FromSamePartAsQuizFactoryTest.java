package com.odde.doughnut.models.quizFacotries;

import com.odde.doughnut.entities.*;
import com.odde.doughnut.models.UserModel;
import com.odde.doughnut.models.randomizers.NonRandomizer;
import com.odde.doughnut.testability.MakeMe;
import org.hibernate.validator.internal.IgnoreForbiddenApisErrors;
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

import static com.odde.doughnut.entities.QuizQuestion.QuestionType.FROM_SAME_PART_AS;
import static com.odde.doughnut.entities.QuizQuestion.QuestionType.WHICH_SPEC_HAS_INSTANCE;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.hamcrest.Matchers.in;
import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(locations = {"classpath:repository.xml"})
@Transactional
class FromSamePartAsQuizFactoryTest {
    @Autowired
    MakeMe makeMe;
    UserModel userModel;
    NonRandomizer randomizer = new NonRandomizer();
    Note top;
    Note perspective;
    Note subjective;
    Note objective;
    Note ugly;
    Note pretty;
    Note tall;
    ReviewPoint reviewPoint;


    @BeforeEach
    void setup() {
        userModel = makeMe.aUser().toModelPlease();
        top = makeMe.aNote("top").byUser(userModel).please();
        perspective = makeMe.aNote("perspective").under(top).please();
        subjective = makeMe.aNote("subjective").under(top).please();
        objective = makeMe.aNote("objective").under(top).please();
        ugly = makeMe.aNote("ugly").under(top).please();
        pretty = makeMe.aNote("pretty").under(top).please();
        tall = makeMe.aNote("tall").under(top).please();
        makeMe.aLink().between(subjective, perspective, Link.LinkType.PART).please();
        makeMe.aLink().between(objective, perspective, Link.LinkType.PART).please();
        Link uglySubjective = makeMe.aLink().between(ugly, subjective, Link.LinkType.TAGGED_BY).please();
        makeMe.aLink().between(tall, objective, Link.LinkType.TAGGED_BY).please();
        reviewPoint = makeMe.aReviewPointFor(uglySubjective).by(userModel).inMemoryPlease();
        makeMe.refresh(top);
    }

    @Test
    void shouldBeInvalidWhenNoInsatnceOfLink() {
        QuizQuestion quizQuestion = buildQuestion();
        assertThat(quizQuestion, nullValue());
    }

    @Nested
    class WhenThereIsAnCousin {

        @BeforeEach
        void setup() {
            makeMe.aLink().between(pretty, subjective, Link.LinkType.TAGGED_BY).please();
            makeMe.refresh(userModel.getEntity());
        }

        @Test
        void shouldIncludeRightAnswers() {
            QuizQuestion quizQuestion = buildQuestion();
            //assertThat(quizQuestion.getDescription(), containsString("<p>Which one is a specialization of <mark>element</mark> <em>and</em> is an instance of <mark>non-official name</mark>:"));
            List<String> strings = toOptionStrings(quizQuestion);
            assertThat(pretty.getTitle(), in(strings));
            assertThat(ugly.getTitle(), not(in(strings)));
        }

    }


    private QuizQuestion buildQuestion() {
        QuizQuestionDirector builder = new QuizQuestionDirector(FROM_SAME_PART_AS, randomizer, reviewPoint, makeMe.modelFactoryService);
        return builder.buildQuizQuestion();
    }

    private List<String> toOptionStrings(QuizQuestion quizQuestion) {
        List<QuizQuestion.Option> options = quizQuestion.getOptions();
        return options.stream().map(QuizQuestion.Option::getDisplay).collect(Collectors.toUnmodifiableList());
    }
}

