/// <reference types="cypress" />
/// <reference types="../support" />
// @ts-check

import { Given, When, Then } from '@badeball/cypress-cucumber-preprocessor'
import start, { mock_services } from '../start'

Given('the browser is mocked to give permission to record audio', () => {
  mock_services.browser.mockAudioRecording()
})

Given(
  'the browser records audio input from the microphone as in {string}',
  (audioFileName: string) => {
    mock_services.browser.receiveAudioFromMicrophone(audioFileName)
  }
)

When('I start recording audio for the note {string}', (noteTopic: string) => {
  start.jumpToNotePage(noteTopic).audioTools().startRecording()
})

When('I stop recording audio', () => {
  start.assumeAudioTools().stopRecording()
})

Then(
  'I must be able to download the audio file to my local machine and it matches the size {int}',
  (_expectedSize: number) => {
    start.assumeAudioTools().downloadAudioFile('recorded_audio.wav')
  }
)
