/// <reference types="cypress" />
/// <reference types="../support" />
// @ts-check

import { Given, When } from '@badeball/cypress-cucumber-preprocessor'
import start from '../start'

Given(
  'the browser is mocked to give permission to record audio and receive audio input as in {string}',
  (audioFileName: string) => {
    // Mock the getUserMedia function to simulate permission granted
    cy.window().then((win) => {
      win.navigator.mediaDevices.getUserMedia = async () => {
        const audio = new Audio()
        return new MediaStream([audio.captureStream().getAudioTracks()[0]])
      }
    })

    cy.fixture(audioFileName, 'base64').then((_audioContent) => {
      cy.window().then((win) => {
        // Mock the MediaRecorder to use the specified audio file
        win.MediaRecorder = class MockMediaRecorder {
          stop() {
            const event = new Event('dataavailable')
            // @ts-ignore
            event.data = new Blob([new ArrayBuffer(0)], { type: 'audio/wav' })
            this.ondataavailable(event)
          }

          static isTypeSupported(type: string): boolean {
            return type === 'audio/wav'
          }
        }
      })
    })

    // Grant microphone permissions
    cy.wrap(
      Cypress.automation('remote:debugger:protocol', {
        command: 'Browser.grantPermissions',
        params: {
          permissions: ['audioCapture'],
          origin: window.location.origin,
        },
      })
    )
  }
)

When('I start recording audio for the note {string}', (noteTopic: string) => {
  start.jumpToNotePage(noteTopic).audioTools().startRecording()
})

When('I stop recording audio', () => {
  start.assumeNotePage().audioTools().stopRecording()
})
