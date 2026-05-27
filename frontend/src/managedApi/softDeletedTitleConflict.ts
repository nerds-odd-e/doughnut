import { toOpenApiError } from "@/managedApi/openApiError"

export type SoftDeletedTitleConflict = {
  message: string
  deletedNoteId?: number
}

const MOVE_BLOCKED_SUFFIX =
  "Undo delete that note, or rename the note you are moving."

function errorBodyFromThrown(e: unknown): unknown {
  const err = e as { body?: unknown }
  return err.body ?? e
}

function softDeletedTitleErrorType(body: object): string | undefined {
  if (
    "errorType" in body &&
    typeof (body as { errorType: unknown }).errorType === "string"
  ) {
    return (body as { errorType: string }).errorType
  }
  return toOpenApiError(body).errorType
}

export function parseSoftDeletedTitleConflict(
  e: unknown
): SoftDeletedTitleConflict | null {
  const body = errorBodyFromThrown(e)
  if (body == null || typeof body !== "object") {
    return null
  }
  if (softDeletedTitleErrorType(body) !== "SOFT_DELETED_TITLE_CONFLICT") {
    return null
  }
  const parsed = toOpenApiError(body)
  const message = parsed.message?.trim()
  if (!message) {
    return null
  }
  const deletedNoteIdRaw = parsed.errors?.deletedNoteId
  const deletedNoteId =
    deletedNoteIdRaw != null ? Number(deletedNoteIdRaw) : undefined
  return {
    message,
    ...(deletedNoteId != null && !Number.isNaN(deletedNoteId)
      ? { deletedNoteId }
      : {}),
  }
}

export function moveBlockedBySoftDeletedTitleMessage(
  conflict: SoftDeletedTitleConflict
): string {
  return `${conflict.message} ${MOVE_BLOCKED_SUFFIX}`
}
