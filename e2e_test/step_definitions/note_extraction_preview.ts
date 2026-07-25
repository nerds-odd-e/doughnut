/// <reference types="cypress" />
/// <reference types="../support" />
// @ts-check

import { Then, When } from '@badeball/cypress-cucumber-preprocessor'
import start from '../start'

When(
  'I extract refinement layout points {string} and {string} to a new note',
  (firstPoint: string, secondPoint: string) => {
    start
      .assumeAssimilationPage()
      .extractLayoutPointsToNewNote(firstPoint, secondPoint)
  }
)

When(
  'I open extraction preview for refinement layout points {string} and {string}',
  (firstPoint: string, secondPoint: string) => {
    start
      .assumeAssimilationPage()
      .openExtractionPreviewForLayoutPoints(firstPoint, secondPoint)
  }
)

When(
  'I open extraction preview on note {string} for refinement layout points {string} and {string}',
  (noteTitle: string, firstPoint: string, secondPoint: string) => {
    start
      .jumpToNotePage(noteTitle)
      .moreOptions()
      .openAssimilationSettings()
      .openExtractionPreviewForLayoutPoints(firstPoint, secondPoint)
  }
)

When('I create the note from the extraction preview', () => {
  start.assumeAssimilationPage().createNoteFromExtractionPreview()
})

When('I retry the extraction preview', () => {
  start.assumeAssimilationPage().retryExtractionPreview()
})

When(
  'I edit the extraction preview to title {string} and content {string} and updated parent content {string}',
  (
    newNoteTitle: string,
    newNoteContent: string,
    updatedOriginalNoteContent: string
  ) => {
    start.assumeAssimilationPage().editExtractionPreviewFields({
      newNoteTitle,
      newNoteContent,
      updatedOriginalNoteContent,
    })
  }
)

When('I clear the extraction preview new note title', () => {
  start.assumeAssimilationPage().clearExtractionPreviewNewNoteTitle()
})

Then('the extraction preview create note button should be disabled', () => {
  start.assumeAssimilationPage().expectExtractionPreviewCreateButtonDisabled()
})

Then('the extraction preview original content tab should be active', () => {
  start
    .assumeAssimilationPage()
    .expectExtractionPreviewOriginalContentTabActive()
})

Then(
  'the extraction preview original content field should contain {string}',
  (content: string) => {
    start
      .assumeAssimilationPage()
      .expectExtractionPreviewOriginalContentFieldContains(content)
  }
)

When('I switch the extraction preview original section to the diff tab', () => {
  start
    .assumeAssimilationPage()
    .switchExtractionPreviewOriginalSectionToDiffTab()
})

Then(
  'the extraction preview original diff should show original {string} and updated {string}',
  (originalContent: string, updatedContent: string) => {
    start
      .assumeAssimilationPage()
      .expectExtractionPreviewOriginalDiffShows(originalContent, updatedContent)
  }
)
