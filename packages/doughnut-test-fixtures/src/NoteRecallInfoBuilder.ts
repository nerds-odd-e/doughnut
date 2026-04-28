import type {
  MemoryTracker,
  NoteRecallInfo,
  NoteRecallSetting,
} from '@generated/doughnut-backend-api'
import Builder from './Builder'

class NoteRecallInfoBuilder extends Builder<NoteRecallInfo> {
  data: NoteRecallInfo

  constructor() {
    super()
    this.data = {
      memoryTrackers: [],
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

  do(): NoteRecallInfo {
    return this.data
  }
}

export default NoteRecallInfoBuilder
