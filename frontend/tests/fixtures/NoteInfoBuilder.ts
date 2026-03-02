import type {
  MemoryTracker,
  NoteInfo,
  NoteRecallSetting,
} from "@generated/backend"
import Builder from "./Builder"

class NoteInfoBuilder extends Builder<NoteInfo> {
  data: NoteInfo

  constructor() {
    super()
    this.data = {
      memoryTrackers: [],
      noteType: undefined,
    }
  }

  memoryTrackers(value: MemoryTracker[]): NoteInfoBuilder {
    this.data.memoryTrackers = value
    return this
  }

  recallSetting(value: NoteRecallSetting): NoteInfoBuilder {
    this.data.recallSetting = value
    return this
  }

  noteType(value: NoteInfo["noteType"]): NoteInfoBuilder {
    this.data.noteType = value
    return this
  }

  do(): NoteInfo {
    return this.data
  }
}

export default NoteInfoBuilder
