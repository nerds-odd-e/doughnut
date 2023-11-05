/// <reference types="cypress" />
/// <reference types="../support" />
// @ts-check

import { Given } from "@badeball/cypress-cucumber-preprocessor"
import start from "start"

Given(
  "I have {int} positive feedbacks and {int} negative feedbacks",
  (positive: number, negative: number) => {
    const positives = Array.from({ length: positive }, (_, index) => ({
      positiveFeedback: true,
      preservedNoteContent: "note content",
      realCorrectAnswers: "",
      preservedQuestion: {
        stem: `good question #${index}`,
        choices: ["choice 1", "choice 2"],
        correctChoiceIndex: 0,
      },
    }))
    const negatives = Array.from({ length: negative }, (_, index) => ({
      positiveFeedback: false,
      preservedNoteContent: "note content",
      realCorrectAnswers: "",
      preservedQuestion: {
        stem: `bad question #${index}`,
        choices: ["choice 1", "choice 2"],
        correctChoiceIndex: 0,
      },
    }))

    start.testability().seedSuggestedQuestions(positives.concat(negatives))
  },
)
