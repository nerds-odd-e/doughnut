import type {
  MemoryTracker,
  NoteRecallInfo,
  NoteRecallSetting,
} from "@generated/backend"
import Builder from "./Builder"

class NoteRecallInfoBuilder extends Builder<NoteRecallInfo> {
  data: NoteRecallInfo

  constructor() {
    super()
    this.data = {
      memoryTrackers: [],
      noteType: undefined,
    }
  }

  memoryTrackers(value: MemoryTracker[]): NoteRecallInfoBuilder {
    this.data.memoryTrackers = value
    return this
  }

  recallSetting(value: NoteRecallSetting): NoteRecallInfoBuilder {
    this.data.recallSetting = value
    return this
  }

  noteType(value: NoteRecallInfo["noteType"]): NoteRecallInfoBuilder {
    this.data.noteType = value
    return this
  }

  do(): NoteRecallInfo {
    return this.data
  }
}

export default NoteRecallInfoBuilder
