import type {
  MemoryTracker,
  NoteRecallInfo,
} from "@generated/donut-backend-api"

export type MemoryTrackerType = NonNullable<MemoryTracker["type"]>

function isNoteLevelMemoryTracker(mt: MemoryTracker) {
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

function matchesTrackerType(mt: MemoryTracker, type: MemoryTrackerType) {
  return type === "UNDERSTANDING"
    ? isUnderstandingMemoryTracker(mt)
    : mt.type === type
}

export function noteLevelTrackerOfType(
  trackers: MemoryTracker[] | undefined,
  type: MemoryTrackerType,
  propertyKey?: string
): MemoryTracker | undefined {
  return trackers?.find(
    (mt) =>
      matchesTrackerGrain(mt, propertyKey) &&
      matchesTrackerType(mt, type) &&
      mt.removedFromTracking !== true
  )
}
