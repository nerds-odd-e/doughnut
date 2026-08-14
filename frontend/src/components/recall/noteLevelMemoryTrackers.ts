import type { MemoryTracker } from "@generated/doughnut-backend-api"

function isNoteLevelMemoryTracker(mt: MemoryTracker) {
  return !mt.propertyKey
}

function isUnderstandingNoteLevelTracker(mt: MemoryTracker) {
  return (
    isNoteLevelMemoryTracker(mt) &&
    mt.type !== "COMMISSIONED" &&
    mt.type !== "SPELLING"
  )
}

function understandingNoteLevelTrackers(
  trackers: MemoryTracker[] | undefined
): MemoryTracker[] {
  return trackers?.filter(isUnderstandingNoteLevelTracker) ?? []
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
  return understandingNoteLevelTrackers(trackers).length > 0
}

export function activeUnderstandingNoteLevelTrackers(
  trackers: MemoryTracker[] | undefined
): MemoryTracker[] {
  return understandingNoteLevelTrackers(trackers).filter(
    (mt) => mt.removedFromTracking !== true
  )
}
