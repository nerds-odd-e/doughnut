import type { PropertyValue } from "@/utils/noteProperties"
import {
  isNoteLevelPropertyKey,
  propertyKeyBaseAndSuffix,
} from "@/utils/noteContentPropertyKeys"

export const AUTHORED_NOTE_LEVEL_MESSAGE =
  "note_level must be an integer from 1 to 6."

function isValidAuthoredNoteLevelScalar(raw: string): boolean {
  return raw.length === 1 && raw >= "1" && raw <= "6"
}

export function authoredNoteLevelValidationErrorForPropertyRow(row: {
  key: string
  value: PropertyValue
}): string | undefined {
  if (!isNoteLevelPropertyKey(row.key)) return
  if (propertyKeyBaseAndSuffix(row.key).suffix != null) {
    return AUTHORED_NOTE_LEVEL_MESSAGE
  }
  if (row.value.kind !== "scalar") {
    return AUTHORED_NOTE_LEVEL_MESSAGE
  }
  if (!isValidAuthoredNoteLevelScalar(row.value.value)) {
    return AUTHORED_NOTE_LEVEL_MESSAGE
  }
  return
}
