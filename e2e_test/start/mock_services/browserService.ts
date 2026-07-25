const browser = {
  audioWorletPort: {
    onmessage: null as ((event: MessageEvent) => void) | null,
  },
  installAudioRecordingMocks(win: Cypress.AUTWindow) {
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
        postMessage: (_message: MessageEventInit, _transfer?: Transferable[]) =>
          undefined,
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
        return
      }
    }
    ;(win as Window & typeof globalThis).AudioWorkletNode =
      MockAudioWorkletNode as unknown as typeof AudioWorkletNode
  },

  stubMediaDevicesForRecording(win: Cypress.AUTWindow) {
    const mockTrack = {
      stop: () => {
        // Placeholder implementation
      },
      getSettings: () => ({ deviceId: 'default' }),
    }
    cy.stub(win.navigator.mediaDevices, 'getUserMedia').resolves({
      getTracks: () => [mockTrack],
      getAudioTracks: () => [mockTrack],
    })
    cy.stub(win.navigator.mediaDevices, 'enumerateDevices').resolves([
      {
        kind: 'audioinput',
        deviceId: 'default',
        label: 'Default microphone',
        groupId: 'default',
        toJSON: () => ({}),
      } as MediaDeviceInfo,
    ])
  },

  mockAudioRecording: function () {
    cy.on('window:before:load', (win: Cypress.AUTWindow) => {
      this.installAudioRecordingMocks(win)
      this.stubMediaDevicesForRecording(win)
    })

    return cy.window().then((win) => {
      this.installAudioRecordingMocks(win)
      this.stubMediaDevicesForRecording(win)
    })
  },
  receiveAudioFromMicrophone: function (audioFileName: string) {
    const targetSampleRate = 16000
    const cached = this.decodedAudioCache[audioFileName]
    if (cached) {
      this.deliverAudioToWorklet(cached)
      return
    }

    cy.fixture(audioFileName, 'base64').then((audioBase64) => {
      const blob = Cypress.Blob.base64StringToBlob(audioBase64, 'audio/wav')
      return blob.arrayBuffer().then((arrayBuffer) => {
        const audioContext = new AudioContext()
        return audioContext.decodeAudioData(arrayBuffer).then((audioBuffer) => {
          const float32Array = audioBuffer.getChannelData(0)
          const resampledBuffer =
            audioBuffer.sampleRate === targetSampleRate
              ? float32Array
              : this.resampleAudio(
                  float32Array,
                  audioBuffer.sampleRate,
                  targetSampleRate
                )
          this.decodedAudioCache[audioFileName] = resampledBuffer
          this.deliverAudioToWorklet(resampledBuffer)
        })
      })
    })
  },

  deliverAudioToWorklet: function (pcmSamples: Float32Array) {
    if (!this.audioWorletPort.onmessage)
      throw new Error(`audioWorletPort.onmessage is not mocked`)
    this.audioWorletPort.onmessage({
      data: { audioBuffer: [pcmSamples] },
    } as MessageEvent)
  },

  decodedAudioCache: {} as Record<string, Float32Array>,

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
