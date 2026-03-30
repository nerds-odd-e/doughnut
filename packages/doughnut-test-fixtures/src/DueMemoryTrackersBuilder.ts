import type {
  DueMemoryTrackers,
  MemoryTrackerLite,
} from '@generated/doughnut-backend-api'
import Builder from './Builder'

class DueMemoryTrackersBuilder extends Builder<DueMemoryTrackers> {
  memoryTrackersToRepeat: MemoryTrackerLite[] = []

  private totalAssimilatedCountToUse = 100

  totalAssimilatedCount(count: number) {
    this.totalAssimilatedCountToUse = count
    return this
  }

  toRepeat(memoryTrackers: MemoryTrackerLite[]) {
    this.memoryTrackersToRepeat = memoryTrackers
    return this
  }

  // eslint-disable-next-line class-methods-use-this
  do(): DueMemoryTrackers {
    return {
      toRepeat: this.memoryTrackersToRepeat,
      dueInDays: 0,
      totalAssimilatedCount: this.totalAssimilatedCountToUse,
      currentRecallWindowEndAt: new Date().toISOString(),
    }
  }
}

export default DueMemoryTrackersBuilder
