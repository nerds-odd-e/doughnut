/// <reference types="cypress" />
/// <reference types="../support" />
// @ts-check

import { Then, When } from '@badeball/cypress-cucumber-preprocessor'
import start from '../start'

When(
  'I open extraction preview for refinement layout items {string} and {string}',
  (firstItem: string, secondItem: string) => {
    start
      .assumeAssimilationPage()
      .openExtractionPreviewForLayoutItems(firstItem, secondItem)
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
