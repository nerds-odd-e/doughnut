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
      cy.stub(win.navigator.mediaDevices, 'getUserMedia').resolves({
        getTracks: () => [
          {
            stop: () => {
              // Placeholder implementation
            },
          },
        ],
      })
    })

    // Mock the MediaRecorder
    cy.on('window:before:load', (win) => {
      // biome-ignore lint/suspicious/noExplicitAny: <explanation>
      let ondataavailable: ((event: any) => void) | null = null
      let onstop: (() => void) | null = null

      class MockMediaRecorder {
        state: 'inactive' | 'recording' | 'paused'
        constructor() {
          this.state = 'inactive'
        }
        start() {
          this.state = 'recording'
        }
        stop() {
          this.state = 'inactive'
          // Simulate ondataavailable event
          if (ondataavailable) {
            cy.get('@audioBlob').then((audioBlob) => {
              ondataavailable?.({ data: audioBlob })
              if (onstop) onstop()
            })
          }
        }
        // biome-ignore lint/suspicious/noExplicitAny: <explanation>
        set ondataavailable(callback: (event: any) => void) {
          ondataavailable = callback
        }
        set onstop(callback: () => void) {
          onstop = callback
        }
      }
      // biome-ignore lint/suspicious/noExplicitAny: <explanation>
      ;(win as any).MediaRecorder = MockMediaRecorder
    })

    // Preload the audio fixture
    cy.fixture(audioFileName, 'base64').then((audioBase64) => {
      const blob = Cypress.Blob.base64StringToBlob(audioBase64, 'audio/wav')
      cy.wrap(blob).as('audioBlob')
    })
  }
)

When('I start recording audio for the note {string}', (noteTopic: string) => {
  start.jumpToNotePage(noteTopic).audioTools().startRecording()
})

When('I stop recording audio', () => {
  start.assumeAudioTools().stopRecording()
})
