import { RecallsController, type DueMemoryTrackers } from 'doughnut-api'
import {
  doughnutSdkOptions,
  runDefaultBackendJson,
} from '../backendApi/doughnutBackendClient.js'

function dueRecallQuery(dueindays: number) {
  return {
    timezone: Intl.DateTimeFormat().resolvedOptions().timeZone,
    dueindays,
  }
}

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
