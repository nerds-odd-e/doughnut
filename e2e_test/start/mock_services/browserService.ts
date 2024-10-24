const browser = {
  mockAudioRecording: (audioFileName: string) => {
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

    // Mock the AudioContext and related audio processing
    cy.on('window:before:load', (win) => {
      class MockAudioContext {
        createMediaStreamSource() {
          return {
            // biome-ignore lint/suspicious/noEmptyBlockStatements: <explanation>
            connect: () => {},
            // biome-ignore lint/suspicious/noEmptyBlockStatements: <explanation>
            disconnect: () => {},
          }
        }
        createScriptProcessor() {
          return {
            // biome-ignore lint/suspicious/noEmptyBlockStatements: <explanation>
            connect: () => {},
            // biome-ignore lint/suspicious/noEmptyBlockStatements: <explanation>
            disconnect: () => {},
            onaudioprocess: null,
          }
        }
        get destination() {
          return {}
        }
      }
      // biome-ignore lint/suspicious/noExplicitAny: <explanation>
      ;(win as any).AudioContext = MockAudioContext
    })

    // Preload the audio fixture
    cy.fixture(audioFileName, 'base64').then((audioBase64) => {
      const blob = Cypress.Blob.base64StringToBlob(audioBase64, 'audio/wav')
      cy.wrap(blob).as('audioBlob')
    })

    // Mock the Float32Array for audio data
    cy.on('window:before:load', (win) => {
      // biome-ignore lint/suspicious/noExplicitAny: <explanation>
      ;(win as any).Float32Array = function MockFloat32Array(length) {
        return new Array(length).fill(0)
      }
    })
  },
}

export default browser
