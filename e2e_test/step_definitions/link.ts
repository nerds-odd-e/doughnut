/// <reference types="cypress" />
/// <reference types="@testing-library/cypress" />
/// <reference types="../support" />
// @ts-check

import { When } from '@badeball/cypress-cucumber-preprocessor'
import start from '../start'

When(
  'I insert a wiki link to {string} via the link toolbar',
  (targetNoteTitle: string) => {
    start.assumeNotePage().insertWikiLinkToNote(targetNoteTitle)
  }
)
