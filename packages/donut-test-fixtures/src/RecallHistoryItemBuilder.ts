import type {
  RecallHistoryItem,
  RecallLog,
  RecallPromptHistoryItem,
} from '@generated/donut-backend-api'
import Builder from './Builder'

class RecallHistoryItemBuilder extends Builder<RecallHistoryItem> {
  private recallLogToUse?: RecallLog
  private recallPromptToUse?: RecallPromptHistoryItem

  recallLog(recallLog: RecallLog): RecallHistoryItemBuilder {
    this.recallLogToUse = recallLog
    return this
  }

  recallPrompt(
    recallPrompt: RecallPromptHistoryItem
  ): RecallHistoryItemBuilder {
    this.recallPromptToUse = recallPrompt
    return this
  }

  do(): RecallHistoryItem {
    return {
      recallLog: this.recallLogToUse,
      recallPrompt: this.recallPromptToUse,
    }
  }
}

export default RecallHistoryItemBuilder
