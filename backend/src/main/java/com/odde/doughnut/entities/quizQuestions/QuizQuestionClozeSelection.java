package com.odde.doughnut.entities.quizQuestions;

import com.odde.doughnut.entities.QuizQuestionEntity;
import com.odde.doughnut.factoryServices.quizFacotries.factories.*;
import com.odde.doughnut.factoryServices.quizFacotries.presenters.*;
import jakarta.persistence.*;
import java.util.*;

@Entity
@DiscriminatorValue("1")
public class QuizQuestionClozeSelection extends QuizQuestionEntity {}
