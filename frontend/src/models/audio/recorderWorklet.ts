const recorderWorkletProcessorCode = `
class RecorderWorkletProcessor extends AudioWorkletProcessor {
  constructor() {
    super();
    this.buffer = [];
    this.totalSamples = 0;
  }

  process(inputs, outputs, parameters) {
    const input = inputs[0];
    if (input.length > 0 && input[0].length > 0) {
      const inputData = input[0]; // Assuming mono channel
      this.buffer.push(new Float32Array(inputData));
      this.totalSamples += inputData.length;

      // Send data to the main thread every 4096 samples
      if (this.totalSamples >= 4096) {
        this.port.postMessage({ audioBuffer: this.buffer });
        this.buffer = [];
        this.totalSamples = 0;
      }
    }
    return true; // Keep the processor alive
  }
}

registerProcessor('recorder-worklet-processor', RecorderWorkletProcessor);
`

export const getAudioRecordingWorkerURL = (): string => {
  const blob = new Blob([recorderWorkletProcessorCode], {
    type: "application/javascript",
  })
  return URL.createObjectURL(blob)
}
