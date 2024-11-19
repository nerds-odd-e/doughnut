import { createAudioFile } from "./createAudioFile"

export function isSilent(data: Float32Array, threshold: number): boolean {
  const sum = data.reduce((acc, val) => acc + Math.abs(val), 0)
  const avg = sum / data.length
  return avg < threshold
}

export function isAllSilent(
  chunks: Float32Array[],
  threshold: number
): boolean {
  return chunks.every((chunk) => isSilent(chunk, threshold))
}

export class AudioBuffer {
  private audioData: Float32Array[] = []
  private lastProcessedArrayIndex = 0
  private lastProcessedInternalIndex = 0

  private isAllSilent(data: Float32Array[], threshold: number): boolean {
    return data.every((chunk) => isSilent(chunk, threshold))
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

  updateProcessedPosition(arrayIndex: number, internalIndex: number): void {
    this.lastProcessedArrayIndex = arrayIndex
    this.lastProcessedInternalIndex = internalIndex
  }

  calculateNewIndices(processedSamples: number): void {
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

  length(): number {
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

  private getProcessableData(silenceThreshold: number): Float32Array[] | null {
    const dataToProcess = this.getUnprocessedData()
    if (
      dataToProcess.length === 0 ||
      this.isAllSilent(dataToProcess, silenceThreshold)
    ) {
      return null
    }
    return dataToProcess
  }

  tryGetProcessableData(
    silenceThreshold: number,
    sampleRate: number
  ): File | null {
    if (this.hasNoUnprocessedData()) return null

    const dataToProcess = this.getProcessableData(silenceThreshold)
    if (!dataToProcess) return null

    return createAudioFile(dataToProcess, sampleRate, true)
  }
}
