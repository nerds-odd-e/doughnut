/// <reference types="cypress" />
/// <reference types="../support" />
// @ts-check

import { Then } from '@badeball/cypress-cucumber-preprocessor'
import type NotePath from '../support/NotePath'
import start from '../start'

Then(
  'the note {notepath} in Donut should have content {string}',
  (notePath: NotePath, expectedContent: string) => {
    start.donutNotebookContent.expectNoteContent(notePath.path, expectedContent)
  }
)

Then(
  'the readme of {notepath} in Donut should be {string}',
  (containerPath: NotePath, expectedReadme: string) => {
    start.donutNotebookContent.expectReadme(containerPath.path, expectedReadme)
  }
)
