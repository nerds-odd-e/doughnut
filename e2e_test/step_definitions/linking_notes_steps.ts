import { Given, Then, When } from '@badeball/cypress-cucumber-preprocessor'
import { start } from '../start'

Given(
  'I add a note {string} under {string}',
  (noteTitle: string, parentTitle: string) => {
    start
      .jumpToNotePage(parentTitle)
      .addChildNote()
      .fillInTitle(noteTitle)
      .save()
  }
)

Then(
  'I should see {string} in the recently changed notes list',
  (noteTitle: string) => {
    cy.get('[data-cy="recently-changed-notes"]').should('contain', noteTitle)
  }
)

When(
  'I select {string} from recently changed notes as target with link type {string}',
  (targetNote: string, linkType: string) => {
    cy.get('[data-cy="recently-changed-notes"]').contains(targetNote).click()
    cy.get('[data-cy="link-type-select"]').select(linkType)
    cy.get('[data-cy="create-link-button"]').click()
  }
)
