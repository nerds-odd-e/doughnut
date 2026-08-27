import type {
  MemoryTracker,
  NoteRecallInfo,
} from "@generated/donut-backend-api"

export function isNoteLevelMemoryTracker(mt: MemoryTracker) {
  return !mt.propertyKey
}

function isUnderstandingMemoryTracker(mt: MemoryTracker) {
  return mt.type !== "COMMISSIONED" && mt.type !== "SPELLING"
}

function matchesTrackerGrain(mt: MemoryTracker, propertyKey?: string) {
  return propertyKey
    ? mt.propertyKey === propertyKey
    : isNoteLevelMemoryTracker(mt)
}

export function hasNoteLevelTrackerOfType(
  trackers: MemoryTracker[] | undefined,
  type: NonNullable<MemoryTracker["type"]>
) {
  return (
    trackers?.some((mt) => isNoteLevelMemoryTracker(mt) && mt.type === type) ??
    false
  )
}

export function hasUnderstandingNoteLevelTracker(
  trackers: MemoryTracker[] | undefined
) {
  return (
    trackers?.some(
      (mt) => isNoteLevelMemoryTracker(mt) && isUnderstandingMemoryTracker(mt)
    ) ?? false
  )
}

export function activeUnderstandingTrackers(
  noteRecallInfo: NoteRecallInfo | null | undefined,
  propertyKey?: string
): MemoryTracker[] {
  return (
    noteRecallInfo?.memoryTrackers?.filter(
      (mt) =>
        matchesTrackerGrain(mt, propertyKey) &&
        isUnderstandingMemoryTracker(mt) &&
        mt.removedFromTracking !== true
    ) ?? []
  )
}

export function assimilateDisabledForProperty(
  noteRecallInfo: NoteRecallInfo | null | undefined,
  propertyKey: string
): boolean {
  return (
    noteRecallInfo?.memoryTrackers?.some(
      (mt) => mt.propertyKey === propertyKey
    ) ?? false
  )
}

export function showRemoveFromRecall(
  noteRecallInfo: NoteRecallInfo | null | undefined,
  propertyKey?: string
): boolean {
  return activeUnderstandingTrackers(noteRecallInfo, propertyKey).length > 0
}
