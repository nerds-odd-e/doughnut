import type { DueMemoryTrackers } from "@/generated/backend"
import Builder from "./Builder"

class DueMemoryTrackersBuilder extends Builder<DueMemoryTrackers> {
  memoryTrackerIdstoRepeat: number[] = []

  toRepeat(memoryTrackerIds: number[]) {
    this.memoryTrackerIdstoRepeat = memoryTrackerIds
    return this
  }

  // eslint-disable-next-line class-methods-use-this
  do(): DueMemoryTrackers {
    return {
      toRepeat: this.memoryTrackerIdstoRepeat,
      dueInDays: 0,
    }
  }
}

export default DueMemoryTrackersBuilder
