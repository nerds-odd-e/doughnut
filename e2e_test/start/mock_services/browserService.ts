const browser = {
  log: [] as string[],
  getLog: function () {
    return this.log
  },
  audioWorletPort: {
    onmessage: null as ((event: MessageEvent) => void) | null,
  },
  mockAudioRecording: function () {
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

    // Return a promise that resolves when the audio worklet is fully set up
    return new Cypress.Promise<void>((resolve) => {
      this.log.push('mocking audio worklet')
      cy.on('window:before:load', (win: Cypress.AUTWindow) => {
        this.log.push('mocking audio context')
        // as of now, AudioContext and AudioWorkletNode are not useable in cypress
        // so we need to mock them.
        // In the future, we should be able to use them in cypress directly
        // and make the test more realistic.

        class MockAudioContext {
          audioWorklet = {
            addModule: cy.stub().resolves(),
          }
          createMediaStreamSource() {
            return {
              // Implement stubs for connect and disconnect
              connect: () => {
                // Stub implementation for connecting audio nodes
                return undefined
              },
              disconnect: () => {
                // Stub implementation for disconnecting audio nodes
                return undefined
              },
            }
          }
          get destination() {
            return {}
          }
        }
        // Remove biome-ignore as we're adding proper type annotation
        ;(win as Window & typeof globalThis).AudioContext = MockAudioContext

        class MockAudioWorkletNode {
          // biome-ignore lint/suspicious/noExplicitAny: <explanation>
          port: any
          constructor() {
            this.port = {}
            Object.defineProperty(this.port, 'onmessage', {
              get: () => {
                return browser.audioWorletPort.onmessage
              },
              set: (handler) => {
                browser.audioWorletPort.onmessage = handler
              },
              configurable: true,
              enumerable: true,
            })
          }
          connect() {
            return undefined
          }
          disconnect() {
            return undefined
          }
        }
        ;(win as Window & typeof globalThis).AudioWorkletNode =
          MockAudioWorkletNode

        this.log.push('mocking audio worklet done')
        resolve()
      })
      cy.url().then((url) => {
        if (url === 'about:blank') {
          cy.visit('/')
        }
      })
    })
  },
  receiveAudioFromMicrophone: function (audioFileName: string) {
    cy.fixture(audioFileName, 'base64').then((audioBase64) => {
      const blob = Cypress.Blob.base64StringToBlob(audioBase64, 'audio/wav')
      return blob.arrayBuffer().then((arrayBuffer) => {
        const audioContext = new AudioContext()
        return audioContext.decodeAudioData(arrayBuffer).then((audioBuffer) => {
          const originalSampleRate = audioBuffer.sampleRate
          const targetSampleRate = 16000 // Assuming the target sample rate is 44.1kHz
          const float32Array = audioBuffer.getChannelData(0)

          // Resample the audio data
          const resampledBuffer = this.resampleAudio(
            float32Array,
            originalSampleRate,
            targetSampleRate
          )

          this.log.push('sending audio to audio worklet')
          if (!this.audioWorletPort.onmessage)
            throw new Error(
              `audioWorletPort.onmessage is not mocked:\n ${this.log}`
            )
          this.audioWorletPort.onmessage({
            data: { audioBuffer: [resampledBuffer] },
          } as MessageEvent)
        })
      })
    })
  },

  // Add this new method to the browser object
  resampleAudio: function (
    audioBuffer: Float32Array,
    fromSampleRate: number,
    toSampleRate: number
  ): Float32Array {
    const ratio = toSampleRate / fromSampleRate
    const newLength = Math.round(audioBuffer.length * ratio)
    const result = new Float32Array(newLength)

    for (let i = 0; i < newLength; i++) {
      const index = i / ratio
      const leftIndex = Math.floor(index)
      const rightIndex = Math.ceil(index)
      const interpolationFactor = index - leftIndex

      if (rightIndex >= audioBuffer.length) {
        result[i] = audioBuffer[leftIndex]
      } else {
        result[i] =
          (1 - interpolationFactor) * audioBuffer[leftIndex] +
          interpolationFactor * audioBuffer[rightIndex]
      }
    }

    return result
  },
}

export default browser
