/// <reference types="cypress" />
/// <reference types="../support" />
// @ts-check

import { Then } from '@badeball/cypress-cucumber-preprocessor'
import start from '../start'

Then(
  'I should see an accidental match reveal for spelling answer {string} with reviewed note {string} and matched note {string}',
  (answer: string, reviewedNoteTitle: string, matchedNoteTitle: string) => {
    start
      .assumeAnsweredQuestionPage()
      .expectAccidentalMatchReveal(answer, reviewedNoteTitle, matchedNoteTitle)
  }
)
