const browser = {
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

    // Mock the AudioContext and related audio processing
    cy.on('window:before:load', (win) => {
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
            // biome-ignore lint/suspicious/noEmptyBlockStatements: <explanation>
            connect: () => {},
            // biome-ignore lint/suspicious/noEmptyBlockStatements: <explanation>
            disconnect: () => {},
          }
        }
        get destination() {
          return {}
        }
      }
      // biome-ignore lint/suspicious/noExplicitAny: <explanation>
      ;(win as any).AudioContext = MockAudioContext

      const port = this.audioWorletPort
      class MockAudioWorkletNode {
        port = port
        // biome-ignore lint/suspicious/noEmptyBlockStatements: <explanation>
        connect() {}
        // biome-ignore lint/suspicious/noEmptyBlockStatements: <explanation>
        disconnect() {}
      }
      // biome-ignore lint/suspicious/noExplicitAny: <explanation>
      ;(win as any).AudioWorkletNode = MockAudioWorkletNode
    })

    // Preload the audio fixture
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

          if (!this.audioWorletPort.onmessage)
            throw new Error('audioWorletPort.onmessage is not mocked')
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
