import type {
  MemoryTracker,
  NoteRecallInfo,
} from "@generated/doughnut-backend-api"
import { MemoryTrackerController } from "@generated/doughnut-backend-api/sdk.gen"
import { apiCallWithLoading } from "@/managedApi/clientSetup"
import { useRecallData } from "@/composables/useRecallData"

export function skippedNoteLevelTrackers(
  noteRecallInfo: NoteRecallInfo | null | undefined
): MemoryTracker[] {
  return (
    noteRecallInfo?.memoryTrackers?.filter(
      (mt) => !mt.propertyKey && mt.removedFromTracking === true
    ) ?? []
  )
}

export function skippedPropertyTracker(
  noteRecallInfo: NoteRecallInfo | null | undefined,
  propertyKey: string
): MemoryTracker | undefined {
  return noteRecallInfo?.memoryTrackers?.find(
    (mt) => mt.propertyKey === propertyKey && mt.removedFromTracking === true
  )
}

export function isSkippedForRecall(
  noteRecallInfo: NoteRecallInfo | null | undefined,
  propertyKey?: string
): boolean {
  if (propertyKey) {
    return skippedPropertyTracker(noteRecallInfo, propertyKey) !== undefined
  }
  return skippedNoteLevelTrackers(noteRecallInfo).length > 0
}

export function trackersToRevive(
  noteRecallInfo: NoteRecallInfo | null | undefined,
  propertyKey?: string
): MemoryTracker[] {
  if (propertyKey) {
    const tracker = skippedPropertyTracker(noteRecallInfo, propertyKey)
    return tracker ? [tracker] : []
  }
  return skippedNoteLevelTrackers(noteRecallInfo)
}

export function useReviveMemoryTracker() {
  const { requestDueRecallsRefresh } = useRecallData()

  const reviveMemoryTrackers = async (
    trackers: MemoryTracker[]
  ): Promise<boolean> => {
    for (const tracker of trackers) {
      if (tracker.id === undefined) {
        return false
      }
      const { error } = await apiCallWithLoading(() =>
        MemoryTrackerController.reEnable({
          path: { memoryTracker: tracker.id },
        })
      )
      if (error) {
        return false
      }
    }
    requestDueRecallsRefresh()
    return true
  }

  return { reviveMemoryTrackers }
}
