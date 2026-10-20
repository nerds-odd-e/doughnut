/// <reference types="cypress" />
/// <reference types="@testing-library/cypress" />
/// <reference types="../support" />
// @ts-check

import { Then, When } from '@badeball/cypress-cucumber-preprocessor'
import start from '../start'

When('I open the Donut menu', () => {
  start.donutMenuPanel()
})

Then('the Donut menu should be a full-width sheet below the page bar', () => {
  start.donutMenuPanel().expectFullWidthSheetBelowPageBar()
})

When('I go to the message center from the Donut menu', () => {
  start.messageCenterIndicator().go()
})

Then('I should be in the message center', () => {
  start.assumeMessageCenterPage()
})

Then('the Donut menu should be anchored below its launcher', () => {
  start.donutMenuPanel().expectAnchoredBelowLauncher()
})
