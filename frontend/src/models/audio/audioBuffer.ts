export class AudioBuffer {
  private audioData: Float32Array[] = []
  private lastProcessedArrayIndex = 0
  private lastProcessedInternalIndex = 0

  getUnprocessedData(): Float32Array[] {
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

  public hasNoUnprocessedData(): boolean {
    return this.length() <= this.getCurrentPosition().arrayIndex
  }
}
