import { createAudioFile } from "./createAudioFile"
import { timestampToSeconds } from "./parseTimestamp"

const SILENCE_THRESHOLD = 0.01

export function isSilent(data: Float32Array): boolean {
  const sum = data.reduce((acc, val) => acc + Math.abs(val), 0)
  const avg = sum / data.length
  return avg < SILENCE_THRESHOLD
}

export function isAllSilent(chunks: Float32Array[]): boolean {
  return chunks.every((chunk) => isSilent(chunk))
}

export class AudioBuffer {
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

  private isAllSilent(data: Float32Array[]): boolean {
    return data.every((chunk) => isSilent(chunk))
  }

  private getUnprocessedData(): Float32Array[] {
    const dataToProcess: Float32Array[] = []
    const firstChunk = this.audioData[this.lastProcessedArrayIndex]

    if (firstChunk) {
      dataToProcess.push(firstChunk.slice(this.lastProcessedInternalIndex))
    }

    dataToProcess.push(
      ...this.audioData.slice(this.lastProcessedArrayIndex + 1)
    )
    return dataToProcess
  }

  push(chunk: Float32Array): void {
    this.audioData.push(chunk)
  }

  getAll(): Float32Array[] {
    return this.audioData
  }

  private updateProcessedPosition(
    arrayIndex: number,
    internalIndex: number
  ): void {
    this.lastProcessedArrayIndex = arrayIndex
    this.lastProcessedInternalIndex = internalIndex
  }

  private calculateNewIndices(processedSamples: number): void {
    let totalSamples = this.lastProcessedInternalIndex

    for (let i = this.lastProcessedArrayIndex; i < this.audioData.length; i++) {
      const arrayLength = this.audioData[i]?.length ?? 0
      if (totalSamples + arrayLength <= processedSamples) {
        totalSamples += arrayLength
        this.lastProcessedArrayIndex = i + 1
        this.lastProcessedInternalIndex = 0
      } else {
        this.lastProcessedArrayIndex = i
        this.lastProcessedInternalIndex = processedSamples - totalSamples
        break
      }
    }
  }

  hasUnprocessedData(): boolean {
    return (
      this.lastProcessedArrayIndex < this.audioData.length ||
      (this.lastProcessedArrayIndex === this.audioData.length - 1 &&
        this.lastProcessedInternalIndex <
          (this.audioData[this.lastProcessedArrayIndex]?.length ?? 0))
    )
  }

  private length(): number {
    return this.audioData.length
  }

  private getCurrentPosition(): { arrayIndex: number; internalIndex: number } {
    return {
      arrayIndex: this.lastProcessedArrayIndex,
      internalIndex: this.lastProcessedInternalIndex,
    }
  }

  private hasNoUnprocessedData(): boolean {
    return this.length() <= this.getCurrentPosition().arrayIndex
  }

  private getProcessableData(): Float32Array[] | null {
    const dataToProcess = this.getUnprocessedData()
    if (dataToProcess.length === 0 || this.isAllSilent(dataToProcess)) {
      return null
    }
    return dataToProcess
  }

  tryGetProcessableData(): File | null {
    if (this.hasNoUnprocessedData()) return null

    const dataToProcess = this.getProcessableData()
    if (!dataToProcess) return null

    return createAudioFile(dataToProcess, this.sampleRate, true)
  }

  updateProcessedIndices(timestamp: string | undefined): void {
    const fallbackIndices = {
      arrayIndex: this.length(),
      internalIndex: 0,
    }

    const processedSeconds = timestamp
      ? timestampToSeconds(timestamp)
      : undefined
    if (processedSeconds === undefined) {
      this.updateProcessedPosition(
        fallbackIndices.arrayIndex,
        fallbackIndices.internalIndex
      )
      return
    }

    const processedSamples = Math.floor(processedSeconds * this.sampleRate)
    this.calculateNewIndices(processedSamples)
  }

  createFinalAudioFile(): File {
    return createAudioFile(this.audioData, this.sampleRate, false)
  }

  private receiveNewChunk(chunk: Float32Array): void {
    if (isSilent(chunk)) {
      this.silenceCounter += chunk.length
      if (this.silenceCounter >= this.SILENCE_DURATION_THRESHOLD) {
        this.onSilenceThresholdReached()
        this.silenceCounter = 0
      }
    } else {
      this.silenceCounter = 0
    }
    this.push(chunk)
  }

  public setOnSilenceThresholdReached(callback: () => void): void {
    this.onSilenceThresholdReached = callback
  }

  receiveAudioData(newData: Float32Array[]): void {
    for (const chunk of newData) {
      this.receiveNewChunk(chunk)
    }
  }
}
