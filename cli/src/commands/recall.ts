import { RecallsController, type DueMemoryTrackers } from 'doughnut-api'
import {
  doughnutSdkOptions,
  runDefaultBackendJson,
} from '../backendApi/doughnutBackendClient.js'
import type {
  CommandDoc,
  InteractiveSlashCommand,
} from './interactiveSlashCommand.js'
import { dueRecallQuery } from './recall/dueRecallQuery.js'
import { RecallJustReviewStage } from './recall/RecallJustReviewStage.js'

export async function recallStatus(signal?: AbortSignal): Promise<string> {
  const trackers = await runDefaultBackendJson<DueMemoryTrackers>(() =>
    RecallsController.recalling({
      query: dueRecallQuery(0),
      ...doughnutSdkOptions(signal),
    })
  )
  const count = trackers.toRepeat?.length ?? 0
  if (count === 1) {
    return '1 note to recall today'
  }
  return `${count} notes to recall today`
}

const recallStatusDoc: CommandDoc = {
  name: '/recall-status',
  usage: '/recall-status',
  description: 'Show how many notes are due for recall today',
}

export const recallStatusSlashCommand: InteractiveSlashCommand = {
  line: '/recall-status',
  doc: recallStatusDoc,
  async run() {
    const assistantMessage = await recallStatus()
    return { assistantMessage }
  },
}

const recallDoc: CommandDoc = {
  name: '/recall',
  usage: '/recall',
  description: 'Recall the next due note (just review when no quiz is pending)',
}

export const recallSlashCommand: InteractiveSlashCommand = {
  line: '/recall',
  doc: recallDoc,
  stageComponent: RecallJustReviewStage,
}
