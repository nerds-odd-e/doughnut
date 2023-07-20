/// <reference types="cypress" />
/// <reference types="../support" />
// @ts-check

import { Given, When, Then } from "@badeball/cypress-cucumber-preprocessor"
import PageObjects from "../support/page_objects"

Then("I do these initial reviews in sequence:", (data) => {
  cy.initialReviewInSequence(data.hashes())
})

Given("It's day {int}, {int} hour", (day: number, hour: number) => {
  cy.testability().backendTimeTravelTo(day, hour)
})

Given("I ask to do more repetition", () => {
  cy.repeatMore()
})

Then("I repeat old {string}", (repeatNotes: string) => {
  cy.goAndRepeatReviewNotes(repeatNotes)
})

Then("I repeat more old {string}", (repeatNotes: string) => {
  cy.repeatReviewNotes(repeatNotes)
})

Then(
  "On day {int} I repeat old {string} and initial review new {string}",
  (day: number, repeatNotes: string, initialNotes: string) => {
    cy.testability().backendTimeTravelTo(day, 8)
    cy.goAndRepeatReviewNotes(repeatNotes)
    cy.initialReviewNotes(initialNotes)
  },
)

Given("I go to the reviews page", () => {
  cy.routerToReviews()
})

Then("I should see that I have old notes to repeat", () => {
  cy.findByRole("button", { name: "Start reviewing old notes" })
})

Then("I should see that I have new notes to learn", () => {
  cy.findByRole("button", { name: "Start reviewing new notes" })
})

Then(
  "On day {int} I should have {string} note for initial review and {string} for repeat",
  (day: number, numberOfInitialReviews: string, numberOfRepeats: string) => {
    cy.testability().timeTravelTo(day, 8)
    cy.routerToReviews()
    cy.contains(numberOfInitialReviews, {
      selector: ".doughnut-ring .initial-review",
    })
    cy.routerToReviews()
    cy.findByText(numberOfInitialReviews, {
      selector: ".number-of-initial-reviews",
    })
    cy.findByText(numberOfRepeats, { selector: ".number-of-repeats" })
  },
)

Then("choose to remove it from reviews", () => {
  cy.findByRole("button", { name: "remove this note from review" }).click()
  cy.findByRole("button", { name: "OK" }).click()
})

Then("it should move to review page", () => {
  cy.url().should("eq", Cypress.config().baseUrl + "/reviews")
})

Then("I initial review {string}", (noteTitle) => {
  cy.initialReviewNotes(noteTitle)
})

Then("I added and learned one note {string} on day {int}", (noteTitle: string, day: number) => {
  cy.testability().seedNotes([{ title: noteTitle }])
  cy.testability().backendTimeTravelTo(day, 8)
  cy.initialReviewNotes(noteTitle)
})

Then("I learned one note {string} on day {int}", (noteTitle: string, day: number) => {
  cy.testability().backendTimeTravelTo(day, 8)
  cy.initialReviewNotes(noteTitle)
})

Then("I am repeat-reviewing my old note on day {int}", (day: number) => {
  cy.testability().backendTimeTravelTo(day, 8)
  cy.routerToRepeatReview()
})

Then("I am learning new note on day {int}", (day: number) => {
  cy.testability().backendTimeTravelTo(day, 8)
  cy.routerToInitialReview()
})

Then("I set the level of {string} to be {int}", (noteTitle: string, level: number) => {
  cy.findNoteTitle(noteTitle)
  cy.formField("Level").then(($control) => {
    cy.wrap($control).within(() => {
      cy.findByRole("button", { name: "" + level }).click()
    })
  })
})

Then("I have selected the choice {string}", (choice: string) => {
  cy.formField(choice).check()
  cy.findByRole("button", { name: "Keep for repetition" }).click()
})

Then("choose to remove it fromm reviews", () => {
  cy.get("#more-action-for-repeat").click()
  cy.findByRole("button", { name: "Remove This Note from Review" }).click()
})

Then("I choose yes I remember", () => {
  cy.yesIRemember()
})

Then(
  "I should be asked cloze deletion question {string} with options {string}",
  (question: string, options: string) => {
    cy.shouldSeeQuizWithOptions([question], options)
  },
)

Then(
  "I should be asked picture question {string} with options {string}",
  (pictureInQuestion: string, options: string) => {
    cy.shouldSeeQuizWithOptions([], options)
  },
)

Then("I should be asked picture selection question {string} with {string}", (question: string) => {
  cy.shouldSeeQuizWithOptions([question], "")
})

Then(
  "I should be asked spelling question {string} from notebook {string}",
  (question: string, notebook: string) => {
    cy.expectBreadcrumb(notebook, false)
    cy.findByText(question).should("be.visible")
  },
)

Then(
  "I should be asked link question {string} {string} with options {string}",
  (noteTitle: string, linkType: string, options: string) => {
    cy.shouldSeeQuizWithOptions([noteTitle, linkType], options)
  },
)

Then("I type my answer {string}", (answer: string) => {
  cy.replaceFocusedTextAndEnter(answer)
})

Then("I choose answer {string}", (noteTitle: string) => {
  cy.findByRole("button", { name: noteTitle }).click()
})

Then("I should see that my answer is correct", () => {
  // checking the css name isn't the best solution
  // but the text changes
  cy.get(".alert-success").should("exist")
})

Then("I should see the information of note {string}", (noteTitle: string) => {
  cy.findNoteTitle(noteTitle)
})

Then("I should see that my answer {string} is incorrect", (answer) => {
  cy.findByText(`Your answer \`${answer}\` is incorrect.`)
})

Then("I should see the repetition is finished: {string}", (yesNo) => {
  cy.findByText("You have finished all repetitions for this half a day!").should(
    yesNo === "yes" ? "exist" : "not.exist",
  )
})

Then("The randomizer always choose the last", () => {
  cy.testability().randomizerAlwaysChooseLast()
})

Then("I should see the info of note {string}", (noteTitle: string, data) => {
  cy.findNoteTitle(noteTitle)
  cy.findByRole("button", { name: "i..." }).click({ force: true })
  const attrs = data.hashes()[0]
  for (const k in attrs) {
    cy.contains(k).findByText(attrs[k]).should("be.visible")
  }
})

Then("I view the last result", () => {
  cy.findByRole("button", { name: "view last result" }).click()
})

Then("I should see the review point is removed from review", () => {
  cy.findByText("This review point has been removed from reviewing.")
})

Then("the choice {string} should be correct", (choice: string) => {
  cy.expectQuestionChoiceToBe(choice, "correct")
})

Then("the choice {string} should be incorrect", (choice: string) => {
  cy.expectQuestionChoiceToBe(choice, "incorrect")
})

When("I ask to generate a question for note {string}", (noteTitle: string) => {
  PageObjects.askQuestionForNote(noteTitle)
})

Then("I should be asked {string}", (expectedtQuestionStem: string) => {
  PageObjects.questionWithStem(expectedtQuestionStem)
})

Then(
  "the question stem generated from the note {string} should be {string}",
  (noteTitle: string, expectedtQuestionStem: string) => {
    PageObjects.askQuestionForNote(noteTitle).questionWithStem(expectedtQuestionStem)
  },
)

Then("I should see the question {string} is disabled", (questionStem: string) => {
  PageObjects.questionWithStem(questionStem).isDisabled()
})
