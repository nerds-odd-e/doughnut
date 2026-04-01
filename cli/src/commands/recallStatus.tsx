import { RecallsController, type DueMemoryTrackers } from 'doughnut-api'
import {
  doughnutSdkOptions,
  runDefaultBackendJson,
} from '../backendApi/doughnutBackendClient.js'
import { AsyncAssistantFetchStage } from './gmail/AsyncAssistantFetchStage.js'
import type {
  CommandDoc,
  InteractiveSlashCommand,
  InteractiveSlashCommandStageProps,
} from './interactiveSlashCommand.js'
import { dueRecallQuery } from './recall/dueRecallQuery.js'

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

function RecallStatusStage({
  onSettled,
  onAbortWithError,
}: InteractiveSlashCommandStageProps) {
  return (
    <AsyncAssistantFetchStage
      spinnerLabel="Loading recall status…"
      runAssistantMessage={(signal) => recallStatus(signal)}
      onSettled={onSettled}
      onAbortWithError={onAbortWithError}
    />
  )
}

const recallStatusDoc: CommandDoc = {
  name: '/recall-status',
  usage: '/recall-status',
  description: 'Show how many notes are due for recall today',
}

export const recallStatusSlashCommand: InteractiveSlashCommand = {
  literal: '/recall-status',
  doc: recallStatusDoc,
  stageComponent: RecallStatusStage,
}
