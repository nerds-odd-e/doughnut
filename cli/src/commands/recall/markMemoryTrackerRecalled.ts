import { MemoryTrackerController } from 'doughnut-api'
import {
  doughnutSdkOptions,
  runDefaultBackendJson,
} from '../../backendApi/doughnutBackendClient.js'

export async function markMemoryTrackerRecalled(
  memoryTrackerId: number,
  successful: boolean,
  signal?: AbortSignal
): Promise<void> {
  await runDefaultBackendJson(() =>
    MemoryTrackerController.markAsRecalled({
      path: { memoryTracker: memoryTrackerId },
      query: { successful },
      ...doughnutSdkOptions(signal),
    })
  )
}
