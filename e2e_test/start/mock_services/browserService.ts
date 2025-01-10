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

    // Return a promise that resolves when the audio worklet is fully set up
    return new Cypress.Promise<void>((resolve) => {
      cy.on('window:before:load', (win: Cypress.AUTWindow) => {
        // as of now, AudioContext and AudioWorkletNode are not useable in cypress
        // so we need to mock them.
        // In the future, we should be able to use them in cypress directly
        // and make the test more realistic.

        class MockAudioContext implements Partial<AudioContext> {
          audioWorklet = {
            addModule: cy.stub().resolves(),
          }
          createMediaStreamSource(_mediaStream: MediaStream) {
            const node = {
              connect: () => ({}) as AudioNode,
              disconnect: () => undefined,
              channelCount: 2,
              channelCountMode: 'explicit' as const,
              channelInterpretation: 'speakers' as const,
              context: this,
              numberOfInputs: 1,
              numberOfOutputs: 1,
              mediaStream: _mediaStream,
            } as unknown as MediaStreamAudioSourceNode
            return node
          }
          get destination(): AudioDestinationNode {
            return {
              maxChannelCount: 2,
              channelCount: 2,
              channelCountMode: 'explicit',
              channelInterpretation: 'speakers',
              context: this,
              numberOfInputs: 1,
              numberOfOutputs: 0,
            } as unknown as AudioDestinationNode
          }
          baseLatency = 0
          outputLatency = 0
          close() {
            return Promise.resolve()
          }
        }
        ;(win as Window & typeof globalThis).AudioContext =
          MockAudioContext as unknown as typeof AudioContext

        class MockAudioWorkletNode implements Partial<AudioWorkletNode> {
          port: MessagePort = {
            onmessage: null,
            onmessageerror: null,
            postMessage: (
              _message: MessageEventInit,
              _transfer?: Transferable[]
            ) => undefined,
            start: () => undefined,
            close: () => undefined,
            addEventListener: (_type: string, _listener: EventListener) =>
              undefined,
            removeEventListener: (_type: string, _listener: EventListener) =>
              undefined,
            dispatchEvent: (_event: Event) => false,
          } as unknown as MessagePort
          parameters = new Map()
          onprocessorerror = null
          constructor(_context: BaseAudioContext, _name: string) {
            Object.defineProperty(this.port, 'onmessage', {
              get: () => browser.audioWorletPort.onmessage,
              set: (handler) => {
                browser.audioWorletPort.onmessage = handler
              },
              configurable: true,
              enumerable: true,
            })
          }
          connect() {
            return {} as AudioNode
          }
          disconnect() {
            return undefined
          }
        }
        ;(win as Window & typeof globalThis).AudioWorkletNode =
          MockAudioWorkletNode as unknown as typeof AudioWorkletNode

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

          if (!this.audioWorletPort.onmessage)
            throw new Error(`audioWorletPort.onmessage is not mocked`)
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

      if (rightIndex >= audioBuffer.length || leftIndex === undefined) {
        result[i] = audioBuffer[leftIndex] ?? 0
      } else {
        result[i] =
          (1 - interpolationFactor) * (audioBuffer[leftIndex] ?? 0) +
          interpolationFactor * (audioBuffer[rightIndex] ?? 0)
      }
    }

    return result
  },
}

export default browser
