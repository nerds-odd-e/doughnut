import type {
  DueMemoryTrackers,
  DueCommissionedMemoryTrackerLite,
  MemoryTrackerLite,
} from '@generated/doughnut-backend-api'
import Builder from './Builder'

class DueMemoryTrackersBuilder extends Builder<DueMemoryTrackers> {
  memoryTrackersToRepeat: MemoryTrackerLite[] = []

  private dueCommissionedTrackers: DueCommissionedMemoryTrackerLite[] = []

  private totalAssimilatedCountToUse = 100

  totalAssimilatedCount(count: number) {
    this.totalAssimilatedCountToUse = count
    return this
  }

  toRepeat(memoryTrackers: MemoryTrackerLite[]) {
    this.memoryTrackersToRepeat = memoryTrackers
    return this
  }

  dueCommissioned(trackers: DueCommissionedMemoryTrackerLite[]) {
    this.dueCommissionedTrackers = trackers
    return this
  }

  // eslint-disable-next-line class-methods-use-this
  do(): DueMemoryTrackers {
    return {
      toRepeat: this.memoryTrackersToRepeat,
      dueCommissioned: this.dueCommissionedTrackers,
      dueInDays: 0,
      totalAssimilatedCount: this.totalAssimilatedCountToUse,
      currentRecallWindowEndAt: new Date().toISOString(),
    }
  }
}

export default DueMemoryTrackersBuilder
