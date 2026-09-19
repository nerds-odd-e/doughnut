import type {
  NoteCreationDto,
  NoteRealm,
  NoteUpdateTitleDto,
} from "@generated/donut-backend-api"
import {
  NoteController,
  NotebookController,
  TextContentController,
} from "@generated/donut-backend-api/sdk.gen"
import {
  toOpenApiError,
  setErrorObjectForFieldErrors,
} from "@/managedApi/openApiError"
import { apiCallWithLoading } from "@/managedApi/clientSetup"

export function toErrorMessage(error: unknown, fallback: string): string {
  if (typeof error === "string") return error
  return error ? (toOpenApiError(error).message ?? fallback) : fallback
}

export function throwStoredApiError(
  error: unknown,
  response: { status?: number } | undefined,
  fallback: string,
  options?: { attachFieldErrors?: boolean }
): never {
  const apiError = new Error(fallback) as Error & {
    body?: unknown
    status?: number
    [key: string]: unknown
  }
  if (error) {
    apiError.body = error
    if (options?.attachFieldErrors) {
      setErrorObjectForFieldErrors(apiError)
    }
    const errorObj = toOpenApiError(error)
    apiError.message = errorObj.message || fallback
    if (response?.status !== undefined) {
      apiError.status = response.status
    } else if (errorObj.errors) {
      apiError.status = 400
    }
  }
  throw apiError
}

export async function updateTextContentRequest(
  noteId: Donut.ID,
  field: "edit title" | "edit content",
  content: string,
  titleReferenceHandling?: NonNullable<NoteUpdateTitleDto["referenceHandling"]>
) {
  if (field === "edit title") {
    const body: NoteUpdateTitleDto = {
      newTitle: content,
      ...(titleReferenceHandling != null
        ? { referenceHandling: titleReferenceHandling }
        : {}),
    }
    const { data, error } = await apiCallWithLoading(() =>
      TextContentController.updateNoteTitle({
        path: { note: noteId },
        body,
      })
    )
    if (error || !data) {
      const fieldErrors = toOpenApiError(error).errors
      if (fieldErrors?.newTitle) {
        throw Object.assign(
          new Error(toErrorMessage(error, "Failed to update note title")),
          { title: fieldErrors.newTitle }
        )
      }
      throw new Error(toErrorMessage(error, "Failed to update note title"))
    }
    return data
  }
  const { data, error } = await apiCallWithLoading(() =>
    TextContentController.updateNoteContent({
      path: { note: noteId },
      body: {
        content,
      },
    })
  )
  if (error || !data) {
    throw new Error(toErrorMessage(error, "Failed to update note content"))
  }
  return data
}

export async function loadNoteRequest(noteId: Donut.ID): Promise<NoteRealm> {
  const { data: noteRealm, error } = await apiCallWithLoading(() =>
    NoteController.showNote({
      path: { note: noteId },
    })
  )
  if (error || !noteRealm) {
    throw new Error(toErrorMessage(error, "Failed to load note"))
  }
  return noteRealm
}

export async function createNoteRequest(
  notebookId: number,
  body: NoteCreationDto
): Promise<NoteRealm> {
  const {
    data: nrwp,
    error,
    response,
  } = await apiCallWithLoading(() =>
    NotebookController.createNoteAtNotebookRoot({
      path: { notebook: notebookId },
      body,
    })
  )
  if (error || !nrwp) {
    throwStoredApiError(error, response, "Failed to create note", {
      attachFieldErrors: true,
    })
  }
  return nrwp
}
