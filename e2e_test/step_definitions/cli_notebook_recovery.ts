import { Given, When, Then } from '@badeball/cypress-cucumber-preprocessor'
import { cli } from '../start/pageObjects/cli'

Given(
  'I retain the learning identity of {string} before recovery',
  (title: string) => cli.notebookRecovery.rememberLearning(title)
)

Given(
  'notebook {string} has an unaccepted live-only note {string} with content:',
  (notebook: string, title: string, content: string) =>
    cli.notebookRecovery.injectLiveOnlyNote(notebook, title, content)
)

When(
  'I review live-only note {string} in notebook {string} using the installed CLI',
  (title: string, notebook: string) =>
    cli.notebookRecovery.reviewLiveOnlyNote(notebook, title)
)

When(
  'I recover the reviewed live edits in notebook {string} using the installed CLI',
  (notebook: string) => cli.notebookRecovery.recoverReviewedLiveEdits(notebook)
)

Then(
  'the learning identity of {string} is retained after recovery and publication',
  (title: string) => cli.notebookRecovery.expectLearningRetained(title)
)
