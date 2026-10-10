/// <reference types="cypress" />
/// <reference types="../support" />
// @ts-check

import { Given, Then, When } from '@badeball/cypress-cucumber-preprocessor'
import start, { mock_services } from '../start'
import noteCreationForm from '../start/pageObjects/forms/noteCreationForm'

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

When('I speak the title', () => {
  noteCreationForm.speakTheTitle()
})

When('I stop speaking the title', () => {
  noteCreationForm.stopSpeakingTheTitle()
})

Then('the Title field should read {string}', (title: string) => {
  noteCreationForm.expectTitle(title)
})

Then(
  'I should be told my speech could not be turned into text, with Retry',
  () => {
    start.assumeAudioTools().expectConversionFailureWithRetry()
  }
)

When('I retry converting my speech', () => {
  start.assumeAudioTools().retry()
})
