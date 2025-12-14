import type { DueMemoryTrackers, MemoryTrackerLite } from "@generated/backend"
import Builder from "./Builder"

class DueMemoryTrackersBuilder extends Builder<DueMemoryTrackers> {
  memoryTrackersToRepeat: MemoryTrackerLite[] = []

  toRepeat(memoryTrackers: MemoryTrackerLite[]) {
    this.memoryTrackersToRepeat = memoryTrackers
    return this
  }

  // eslint-disable-next-line class-methods-use-this
  do(): DueMemoryTrackers {
    return {
      toRepeat: this.memoryTrackersToRepeat,
      dueInDays: 0,
      totalAssimilatedCount: 100,
      currentRecallWindowEndAt: new Date().toISOString(),
    }
  }
}

export default DueMemoryTrackersBuilder
