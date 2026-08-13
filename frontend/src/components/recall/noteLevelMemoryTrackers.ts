import type { MemoryTracker } from "@generated/doughnut-backend-api"

function isNoteLevelMemoryTracker(mt: MemoryTracker) {
  return !mt.propertyKey
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
      (mt) =>
        isNoteLevelMemoryTracker(mt) &&
        mt.type !== "COMMISSIONED" &&
        mt.type !== "SPELLING"
    ) ?? false
  )
}
