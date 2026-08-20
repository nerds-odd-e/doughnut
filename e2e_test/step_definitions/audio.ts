/// <reference types="cypress" />
/// <reference types="../support" />
// @ts-check

import { Given, When } from '@badeball/cypress-cucumber-preprocessor'
import start, { mock_services } from '../start'

Given('the browser is mocked to give permission to record audio', () => {
  return mock_services.browser.mockAudioRecording()
})

Given(
  'the browser records audio input from the microphone as in {string}',
  (audioFileName: string) => {
    cy.wrap(null).then(() => {
      mock_services.browser.receiveAudioFromMicrophone(audioFileName)
    })
  }
)

When(
  'I start recording audio for the note {string}',
  (noteTopology: string) => {
    start.jumpToNotePage(noteTopology).audioTools().startRecording()
  }
)

When('I stop recording audio', () => {
  start.assumeAudioTools().stopRecording()
})
