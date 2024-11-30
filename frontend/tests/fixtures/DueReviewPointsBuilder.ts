import type { DueMemoryTrackers } from "@/generated/backend"
import Builder from "./Builder"

class RepetitionBuilder extends Builder<DueMemoryTrackers> {
  reviewPointIdstoRepeat: number[] = []

  toRepeat(reviewPointIds: number[]) {
    this.reviewPointIdstoRepeat = reviewPointIds
    return this
  }

  // eslint-disable-next-line class-methods-use-this
  do(): DueMemoryTrackers {
    return {
      toRepeat: this.reviewPointIdstoRepeat,
      dueInDays: 0,
    }
  }
}

export default RepetitionBuilder
