import type { PropertyValue } from "@/utils/noteProperties"
import type { MemoryTracker } from "@generated/donut-backend-api"

export type MemoryTrackerType = NonNullable<MemoryTracker["type"]>

function isNoteLevelMemoryTracker(mt: MemoryTracker) {
  return !mt.propertyKey
}

function isUnderstandingMemoryTracker(mt: MemoryTracker) {
  return mt.type !== "COMMISSIONED" && mt.type !== "SPELLING"
}

function matchesTrackerGrain(
  mt: MemoryTracker,
  propertyKey?: string,
  propertyValue = ""
) {
  return propertyKey
    ? mt.propertyKey === propertyKey &&
        (mt.propertyValue ?? "") === propertyValue
    : isNoteLevelMemoryTracker(mt)
}

function matchesTrackerType(mt: MemoryTracker, type: MemoryTrackerType) {
  return type === "UNDERSTANDING"
    ? isUnderstandingMemoryTracker(mt)
    : mt.type === type
}

export function noteLevelTrackerOfType(
  trackers: MemoryTracker[] | undefined,
  type: MemoryTrackerType,
  propertyKey?: string,
  propertyValue?: string
): MemoryTracker | undefined {
  return trackers?.find(
    (mt) =>
      matchesTrackerGrain(mt, propertyKey, propertyValue) &&
      matchesTrackerType(mt, type) &&
      mt.removedFromTracking !== true
  )
}

const MAX_TRACKED_PROPERTY_VALUE_LENGTH = 255

/**
 * The values of a list property that are assimilated one by one, following the
 * backend property index: blank, over-long and repeated items are left out.
 * Undefined for a single (scalar) value, which is assimilated as a whole.
 */
export function assimilableListValues(
  value: PropertyValue
): string[] | undefined {
  if (value.kind !== "list") return undefined
  return [
    ...new Set(
      value.items.filter(
        (item) =>
          item.trim() !== "" && item.length <= MAX_TRACKED_PROPERTY_VALUE_LENGTH
      )
    ),
  ]
}
