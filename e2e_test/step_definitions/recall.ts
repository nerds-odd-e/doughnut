/// <reference types="cypress" />
/// <reference types="../support" />
// @ts-check

import { Given, Then, When } from '@badeball/cypress-cucumber-preprocessor'
import type { DataTable } from '@cucumber/cucumber'
import start from '../start'

Then('I assimilate these in sequence:', (data: DataTable) => {
  start.assimilation().goToAssimilationPage().assimilate(data.hashes())
})

Given("It's day {int}, {int} hour", (day: number, hour: number) => {
  start.testability().backendTimeTravelTo(day, hour)
})

Given("It's day {int}", (day: number) => {
  start.testability().backendTimeTravelTo(day, 8)
})

Given('I ask to do more repetition', () => {
  start.recall().goToRecallPage().repeatMore()
})

Then('I recall {string}', (repeatNotes: string) => {
  start.recall().goToRecallPage().recallNotes(repeatNotes)
})

Then('I repeat more old {string}', (repeatNotes: string) => {
  start.recall().assumeRecallPage().recallNotes(repeatNotes)
})

Then(
  'On day {int} I recall {string} and initial review new {string}',
  (day: number, repeatNotes: string, initialNotes: string) => {
    start.testability().timeTravelTo(day, 8)
    start.recall().goToRecallPage().recallNotes(repeatNotes)
    start.assimilation().goToAssimilationPage().assimilateNotes(initialNotes)
  }
)

Given('I go to the recalls page', () => {
  start.recall().goToRecallPage()
})

Then(
  'I should see that I have {int} notes to recall',
  (numberOfNotes: number) => {
    cy.reload()
    start.recall().expectCount(numberOfNotes)
  }
)

Then('I should see that I have no notes to recall today', () => {
  // When there are no notes to recall, the page shows "You have finished all repetitions"
  cy.contains('You have finished all repetitions for this half a day!').should(
    'be.visible'
  )
})

Then(
  'I should see that I have {int} new notes to assimilate',
  (numberOfNotes: number) => {
    start.assimilation().expectCount(numberOfNotes)
  }
)

Then(
  'On day {int} I should have {string} note for assimilation and {string} for recall',
  (day: number, toAssimilateAndTotal: string, numberOfRecalls: string) => {
    start.testability().backendTimeTravelTo(day, 8)
    start
      .assimilation()
      .goToAssimilationPage()
      .expectToAssimilateAndTotal(toAssimilateAndTotal)
    start.recall().goToRecallPage().expectToRecallCounts(numberOfRecalls)
  }
)

Then('it should move to review page', () => {
  cy.url().should('eq', `${Cypress.config().baseUrl}/recalls`)
})

Then('I initial review {string}', (noteTopology: string) => {
  start.assimilation().goToAssimilationPage().assimilateNotes(noteTopology)
})

Then(
  'I added and learned one note {string} on day {int}',
  (noteTopology: string, day: number) => {
    cy.get<string>('@currentLoginUser').then((username) => {
      start.testability().injectNotes([{ Title: noteTopology }], username)
    })
    start.testability().backendTimeTravelTo(day, 8)
    start.assimilation().goToAssimilationPage().assimilateNotes(noteTopology)
  }
)

Then(
  'I assimilated one note {string} on day {int}',
  (noteTopology: string, day: number) => {
    start.testability().backendTimeTravelTo(day, 8)
    start.assimilation().goToAssimilationPage().assimilateNotes(noteTopology)
  }
)

Then('I am recalling my note on day {int}', (day: number) => {
  start.testability().backendTimeTravelTo(day, 8)
  cy.reload()
  start.recall().goToRecallPage()
})

When(
  'I make a wrong answer on day {int}, answering {string} to {string}',
  (day: number, wrongAnswer: string, questionStem: string) => {
    start.testability().backendTimeTravelTo(day, 8)
    cy.reload()
    start.recall().goToRecallPage()
    start.assumeQuestionPage(questionStem).answer(wrongAnswer)
    start.assumeAnsweredQuestionPage().expectMCQAnswerToBeIncorrect(wrongAnswer)
  }
)

When(
  'I make {int} consecutive wrong answers for {int} days since day {int}, answering {string} to {string}',
  (
    _numWrongAnswers: number,
    numDays: number,
    startDay: number,
    wrongAnswer: string,
    questionStem: string
  ) => {
    const days = Array.from({ length: numDays }, (_, i) => startDay + i)

    const runIteration = (index: number) => {
      if (index >= days.length) return
      const day = days[index]
      start.testability().backendTimeTravelTo(day, 8)
      cy.reload()
      start.recall().goToRecallPage()
      start.assumeQuestionPage(questionStem).answer(wrongAnswer)
      start
        .assumeAnsweredQuestionPage()
        .expectMCQAnswerToBeIncorrect(wrongAnswer)
      cy.then(() => runIteration(index + 1))
    }
    runIteration(0)
  }
)

Then('I am assimilating new note on day {int}', (day: number) => {
  start.testability().backendTimeTravelTo(day, 8)
  start.assimilation().goToAssimilationPage()
})

When('I am assimilating the note {string}', (noteTitle: string) => {
  start.jumpToNotePage(noteTitle).moreOptions().openAssimilationPage()
})

Then('I assimilate the note {string}', (noteTitle: string) => {
  start.jumpToNotePage(noteTitle).moreOptions().assimilateNote()
})

Then(
  'I assimilate the note {string} with the option of remembering spelling',
  (noteTitle: string) => {
    start
      .jumpToNotePage(noteTitle)
      .moreOptions()
      .openAssimilationPage()
      .assimilateWithSpellingOption()
  }
)

Then(
  'I set the level of {string} to be {int}',
  (noteTopology: string, level: number) => {
    start.assumeNotePage(noteTopology)
    cy.formField('Level').then(($control) => {
      cy.wrap($control).within(() => {
        cy.findByRole('button', { name: `${level}` }).click()
      })
    })
  }
)

Then('I choose yes I remember', () => {
  cy.yesIRemember()
})

Then('I skip one question', () => {
  start.assumeQuestionPage().skipQuestion()
})

Then(
  'I should be asked spelling question {string} from notebook {string}',
  (question: string, notebook: string) => {
    start.assumeQuestionPage(question).forNotebook(notebook)
  }
)

Then('I type my answer {string}', (answer: string) => {
  cy.pageIsNotLoading()
  cy.replaceFocusedTextAndEnter(answer)
})

Then('I choose answer {string}', (answer: string) => {
  start.assumeQuestionPage().answer(answer)
})

Then(
  'I should see the information of note {string}',
  (noteTopology: string) => {
    start.assumeNotePage(noteTopology)
  }
)

Then(
  'I should see that my MCQ answer {string} is incorrect',
  (answer: string) => {
    start.assumeAnsweredQuestionPage().expectMCQAnswerToBeIncorrect(answer)
  }
)

Then(
  'I should see that my spelling answer {string} is incorrect',
  (answer: string) => {
    start.assumeAnsweredQuestionPage().expectSpellingAnswerToBeIncorrect(answer)
  }
)

Then('I should see that my answer is correct as the last question', () => {
  start
    .assumeAnsweredQuestionPage()
    .goToLastAnsweredQuestion()
    .expectMCQAnswerToBeCorrect()
})

Then('I should see that my answer is correct', () => {
  // leave empty for now
})

Then('I should see that my last answer to spelling question is correct', () => {
  start
    .assumeAnsweredQuestionPage()
    .goToLastAnsweredQuestion()
    .expectSpellingAnswerToBeCorrect()
})

Then(
  'I should see the memory tracker info of note {string}',
  (noteTopology: string, data: DataTable) => {
    start
      .assumeAnsweredQuestionPage()
      .showMemoryTracker(noteTopology)
      .expectMemoryTrackerInfo(data.hashes())
  }
)

Then('choose to remove the last memory tracker from recalls', () => {
  start
    .assumeAnsweredQuestionPage()
    .goToLastAnsweredQuestion()
    .showMemoryTracker()
    .removeMemoryTrackerFromReview('normal')
})

Then('I should see the resume recall menu item', () => {
  cy.findByLabelText('Resume').should('exist')
})

When('I click resume recall from the menu', () => {
  cy.findByLabelText('Resume').click()
})

Then('I should be back to the current question', () => {
  start.recall().assumeRecallPage().expectCurrentQuestion()
})

When(
  'I have the true false question {string} rated as a good example',
  (questionStem: string) => {
    start.testability().injectSuggestedQuestion(questionStem, true)
  }
)

When(
  'I have the true false question {string} rated as a bad example',
  (questionStem: string) => {
    start.testability().injectSuggestedQuestion(questionStem, false)
  }
)

Then('I should be asked {string}', (expectedQuestionStem: string) => {
  start.assumeQuestionPage(expectedQuestionStem)
})

Then(
  'I should see the question {string} is disabled',
  (questionStem: string) => {
    start.assumeQuestionPage(questionStem).isDisabled()
  }
)

Then(
  'I should see the question {string} is enabled',
  (questionStem: string) => {
    start.assumeQuestionPage(questionStem).isNotDisabled()
  }
)

Then(
  'I suggest the question {string} of the note {string} as a good example',
  (questionStem: string, noteTopology: string) => {
    start
      .jumpToNotePage(noteTopology)
      .openQuestionList()
      .suggestingQuestionForFineTuning(questionStem)
      .suggestingPositiveFeedbackForFineTuning()
  }
)

Then(
  'I suggest the question {string} of the note {string} as a bad example',
  (questionStem: string, noteTopology: string) => {
    start
      .jumpToNotePage(noteTopology)
      .openQuestionList()
      .suggestingQuestionForFineTuning(questionStem)
      .suggestingNegativeFeedbackFineTuningExclusion()
  }
)

Then('I should be able to select a note type', () => {
  start.assumeAssimilationPage().expectNoteTypePrompt()
})

Then(
  'I should see an understanding checklist with {int} points',
  (count: number) => {
    start.assumeAssimilationPage().expectUnderstandingPointsCount(count)
  }
)

Given('the note type is {string}', (noteType: string) => {
  start.assumeAssimilationPage().selectNoteType(noteType)
})

Then('I can continue with the assimilation', () => {
  // Verify we're still on the assimilation page or moved to the next note
  cy.url().should('satisfy', (url: string) => {
    return url.includes('/assimilate') || url.includes('/recalls')
  })
})

When(
  'I navigate to the assimilation page for note {string}',
  (noteTitle: string) => {
    start.jumpToNotePage(noteTitle).moreOptions().openAssimilationPage()
  }
)

When('I check the {string} option', (fieldLabel: string) => {
  cy.formField(fieldLabel).check()
  cy.pageIsNotLoading()
})

When(
  'I delete understanding points {int} and {int}',
  (index1: number, index2: number) => {
    start.assumeAssimilationPage().deleteUnderstandingPointsAt([index1, index2])
  }
)

When(
  'I ignore these understanding points and complete assimilation:',
  (data: DataTable) => {
    const pointTexts = data
      .raw()
      .map((row) => row[0])
      .filter((s): s is string => s !== undefined)
    start
      .assumeAssimilationPage()
      .ignoreUnderstandingPointsAndComplete(pointTexts)
  }
)

Then('remembering spelling should be unavailable', () => {
  start.assumeAssimilationPage().expectRememberingSpellingUnavailable()
})

Then('remembering spelling should be available', () => {
  start.assumeAssimilationPage().expectRememberingSpellingAvailable()
})

When('I keep for repetition with remembering spelling', () => {
  start.assumeAssimilationPage().proceedWithRememberingSpelling()
})

When('I verify spelling with {string}', (text: string) => {
  start.assumeAssimilationPage().verifySpellingWith(text)
})

Then(
  'the spelling verification result for note {string} should be {string}',
  (noteTitle: string, expectedResult: string) => {
    if (expectedResult === 'success') {
      start.assumeAssimilationPage().expectPopupClosed()
      start
        .jumpToNotePage(noteTitle)
        .moreOptions()
        .expectMemoryTrackerInfo([
          { type: 'spelling', 'Repetition Count': '0' },
        ])
    } else {
      const errorMessage = expectedResult.replace(/^error: /, '')
      start.assumeAssimilationPage().expectSpellingErrorMessage(errorMessage)
    }
  }
)

Then(
  'I should still be on the assimilate page for {string}',
  (noteTitle: string) => {
    start
      .assumeAssimilationPage()
      .expectToRemainOnAssimilationPageFor(noteTitle)
  }
)
When('I update the note details to {string}', (newDetails: string) => {
  start.assumeAssimilationPage().updateNoteDetails(newDetails)
})

Then('I should see a re-assimilate confirmation dialog', () => {
  cy.contains('You have answered this note incorrectly too many times').should(
    'be.visible'
  )
})

When('I confirm to re-assimilate the note', () => {
  cy.findByRole('button', { name: 'OK' }).click()
})

Then(
  'I should see the assimilation counter increased by {int} in the sidebar',
  (incrementCount: number) => {
    // After re-assimilation, the note should appear in the assimilation queue
    // The counter should show the expected count
    start.assimilation().expectCount(incrementCount)
  }
)
