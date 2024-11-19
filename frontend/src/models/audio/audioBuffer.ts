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
  private audioData: Float32Array[] = []
  private lastProcessedArrayIndex = 0
  private lastProcessedInternalIndex = 0

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

  tryGetProcessableData(sampleRate: number): File | null {
    if (this.hasNoUnprocessedData()) return null

    const dataToProcess = this.getProcessableData()
    if (!dataToProcess) return null

    return createAudioFile(dataToProcess, sampleRate, true)
  }

  updateProcessedIndices(
    timestamp: string | undefined,
    sampleRate: number
  ): void {
    const fallbackIndices = {
      arrayIndex: this.length(),
      internalIndex: 0,
    }

    if (!timestamp) {
      this.updateProcessedPosition(
        fallbackIndices.arrayIndex,
        fallbackIndices.internalIndex
      )
      return
    }

    const processedSeconds = timestampToSeconds(timestamp)
    if (processedSeconds === undefined) {
      this.updateProcessedPosition(
        fallbackIndices.arrayIndex,
        fallbackIndices.internalIndex
      )
      return
    }

    const processedSamples = Math.floor(processedSeconds * sampleRate)
    this.calculateNewIndices(processedSamples)
  }
}
