import type { AudioChunk } from "../audioProcessingScheduler"
import { createAudioFile } from "./createAudioFile"
import { timestampToSeconds } from "../parseTimestamp"
import type { AudioBuffer } from "../audioReceiver"

const SILENCE_THRESHOLD = 0.01

function isSilent(data: Float32Array): boolean {
  const sum = data.reduce((acc, val) => acc + Math.abs(val), 0)
  const avg = sum / data.length
  return avg < SILENCE_THRESHOLD
}

class RawAudioBuffer implements AudioBuffer {
  private readonly SILENCE_DURATION_THRESHOLD: number
  private audioData: Float32Array[] = []
  private lastProcessedArrayIndex = 0
  private lastProcessedInternalIndex = 0
  public readonly sampleRate: number
  public silenceCounter = 0
  private onSilenceThresholdReached: () => void = () => {
    /* intentionally empty */
  }

  constructor(sampleRate: number) {
    this.sampleRate = sampleRate
    this.SILENCE_DURATION_THRESHOLD = 3 * sampleRate
  }

  private getUnprocessedData(): Float32Array[] {
    const unprocessedData: Float32Array[] = []
    const currentIndex = this.lastProcessedArrayIndex

    if (currentIndex >= this.audioData.length) {
      return unprocessedData
    }

    const currentChunk = this.audioData[currentIndex]
    if (currentChunk) {
      const unprocessedFirstChunk = currentChunk.slice(
        this.lastProcessedInternalIndex
      )
      if (unprocessedFirstChunk.length > 0) {
        unprocessedData.push(unprocessedFirstChunk)
      }
    }

    const remainingChunks = this.audioData.slice(currentIndex + 1)
    unprocessedData.push(...remainingChunks)

    return unprocessedData
  }

  hasUnprocessedData(): boolean {
    const currentIndex = this.lastProcessedArrayIndex

    if (currentIndex >= this.audioData.length) {
      return false
    }

    const currentChunk = this.audioData[currentIndex]
    const currentChunkLength = currentChunk?.length ?? 0

    if (this.lastProcessedInternalIndex < currentChunkLength) {
      return true
    }

    return currentIndex + 1 < this.audioData.length
  }

  private getProcessableData(): Float32Array[] | null {
    const dataToProcess = this.getUnprocessedData()
    return dataToProcess.length === 0 || dataToProcess.every(isSilent)
      ? null
      : dataToProcess
  }

  createFinalAudioFile(): File {
    return createAudioFile(this.audioData, this.sampleRate, false)
  }

  public setOnSilenceThresholdReached(callback: () => void): void {
    this.onSilenceThresholdReached = callback
  }

  private push(chunk: Float32Array): void {
    if (isSilent(chunk)) {
      this.silenceCounter += chunk.length
      if (this.silenceCounter >= this.SILENCE_DURATION_THRESHOLD) {
        this.onSilenceThresholdReached()
        this.silenceCounter = 0
      }
    } else {
      this.silenceCounter = 0
    }
    this.audioData.push(chunk)
  }

  receiveAudioData(newData: Float32Array[]): void {
    newData.forEach((chunk) => this.push(chunk))
  }

  async processUnprocessedData(
    processorCallback: (chunk: AudioChunk) => Promise<string | undefined>,
    isMidSpeech = true
  ): Promise<void> {
    const dataToProcess = this.getProcessableData()
    if (!dataToProcess) return

    const startArrayIndex = this.lastProcessedArrayIndex
    const startInternalIndex = this.lastProcessedInternalIndex
    const snapshotLength = this.audioData.length

    const file = createAudioFile(dataToProcess, this.sampleRate, true)
    const timestamp = await processorCallback({ data: file, isMidSpeech })

    const processedSeconds = timestamp
      ? timestampToSeconds(timestamp)
      : undefined
    if (processedSeconds === undefined) {
      this.lastProcessedArrayIndex = snapshotLength
      this.lastProcessedInternalIndex = 0
      return
    }

    const processedSamples = Math.floor(processedSeconds * this.sampleRate)

    let totalSamples = 0
    for (let i = startArrayIndex; i < snapshotLength; i++) {
      const chunk = this.audioData[i]
      if (!chunk) continue
      const chunkLength = chunk.length
      const startIdx = i === startArrayIndex ? startInternalIndex : 0
      const remainingSamplesInChunk = chunkLength - startIdx

      if (totalSamples + remainingSamplesInChunk <= processedSamples) {
        totalSamples += remainingSamplesInChunk
        this.lastProcessedArrayIndex = i + 1
        this.lastProcessedInternalIndex = 0
      } else {
        this.lastProcessedArrayIndex = i
        this.lastProcessedInternalIndex =
          startIdx + (processedSamples - totalSamples)
        break
      }
    }
  }

  getCurrentAverageSample(): number {
    const dataLength = this.audioData.length
    const bufferLength = 3
    const start = dataLength > bufferLength ? dataLength - bufferLength : 0
    const data = this.audioData.slice(start, dataLength)

    // Compute average of data
    let sum = 0
    for (let i = 0; i < data.length; i++) {
      const channel = data[i]
      if (!channel) continue
      for (let j = 0; j < channel.length; j++) {
        sum += channel[j]!
      }
    }

    return sum / data.length
  }
}

export const createAudioBuffer = (sampleRate: number) =>
  new RawAudioBuffer(sampleRate)
