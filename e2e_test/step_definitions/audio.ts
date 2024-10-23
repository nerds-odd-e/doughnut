/// <reference types="cypress" />
/// <reference types="../support" />
// @ts-check

import { Given, When } from '@badeball/cypress-cucumber-preprocessor'
import start, { mock_services } from '../start'

Given(
  'the browser is mocked to give permission to record audio and receive audio input as in {string}',
  (audioFileName: string) => {
    mock_services.browser.mockAudioRecording(audioFileName)
  }
)

When('I start recording audio for the note {string}', (noteTopic: string) => {
  start.jumpToNotePage(noteTopic).audioTools().startRecording()
})

When('I stop recording audio', () => {
  start.assumeAudioTools().stopRecording()
})
