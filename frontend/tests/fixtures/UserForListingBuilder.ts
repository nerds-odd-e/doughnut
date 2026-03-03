import type { UserForListing } from "@generated/backend"
import Builder from "./Builder"
import generateId from "./generateId"

export default class UserForListingBuilder extends Builder<UserForListing> {
  data: UserForListing

  constructor() {
    super()
    const id = generateId()
    this.data = {
      id,
      name: `User ${id}`,
      noteCount: 0,
      memoryTrackerCount: 0,
      lastNoteTime: undefined,
      lastAssimilationTime: undefined,
      lastRecallTime: undefined,
    }
  }

  do(): UserForListing {
    return this.data
  }

  withName(name: string): this {
    this.data.name = name
    return this
  }

  withNoteCount(count: number): this {
    this.data.noteCount = count
    return this
  }

  withMemoryTrackerCount(count: number): this {
    this.data.memoryTrackerCount = count
    return this
  }

  withLastNoteTime(time: string): this {
    this.data.lastNoteTime = time
    return this
  }

  withLastAssimilationTime(time: string): this {
    this.data.lastAssimilationTime = time
    return this
  }

  withLastRecallTime(time: string): this {
    this.data.lastRecallTime = time
    return this
  }
}
