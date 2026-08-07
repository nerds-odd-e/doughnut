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

Then('I cannot create a note from the extraction preview', () => {
  start.assumeAssimilationPage().expectCannotCreateNoteFromExtractionPreview()
})

Then(
  'the extraction preview should show original content {string}',
  (content: string) => {
    start
      .assumeAssimilationPage()
      .expectExtractionPreviewShowsOriginalContent(content)
  }
)

When('I view the extraction preview original as a diff', () => {
  start.assumeAssimilationPage().viewExtractionPreviewOriginalAsDiff()
})

Then(
  'the extraction preview original diff should show original {string} and updated {string}',
  (originalContent: string, updatedContent: string) => {
    start
      .assumeAssimilationPage()
      .expectExtractionPreviewOriginalDiffShows(originalContent, updatedContent)
  }
)
