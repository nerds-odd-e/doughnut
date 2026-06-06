import type {
  MemoryTracker,
  NoteRecallInfo,
} from "@generated/doughnut-backend-api"
import {
  MemoryTrackerController,
  NoteController,
} from "@generated/doughnut-backend-api/sdk.gen"
import usePopups from "@/components/commons/Popups/usePopups"
import { toOpenApiError } from "@/managedApi/openApiError"
import type { PropertyKeyChange } from "@/utils/noteContentFrontmatter"

function findPropertyMemoryTracker(
  noteInfo: NoteRecallInfo | undefined,
  propertyKey: string
): MemoryTracker | undefined {
  return noteInfo?.memoryTrackers?.find(
    (tracker) => tracker.propertyKey && tracker.propertyKey === propertyKey
  )
}

export function usePropertyMemoryTrackerGuard(
  noteId: () => number | undefined
) {
  const { popups } = usePopups()
  let noteInfoCache: NoteRecallInfo | undefined
  let noteInfoLoadPromise: Promise<NoteRecallInfo | undefined> | undefined

  const invalidateNoteInfoCache = () => {
    noteInfoCache = undefined
    noteInfoLoadPromise = undefined
  }

  const loadNoteInfo = async (): Promise<NoteRecallInfo | undefined> => {
    const id = noteId()
    if (id === undefined) {
      return
    }

    if (noteInfoCache !== undefined) {
      return noteInfoCache
    }

    if (noteInfoLoadPromise) {
      return noteInfoLoadPromise
    }

    noteInfoLoadPromise = (async () => {
      const { data, error } = await NoteController.getNoteInfo({
        path: { note: id },
      })
      if (!error) {
        noteInfoCache = data!
      }
      return noteInfoCache
    })()

    return noteInfoLoadPromise
  }

  const confirmAndApplyRemoval = async (
    propertyKey: string
  ): Promise<boolean> => {
    if (noteId() === undefined) {
      return true
    }

    const noteInfo = await loadNoteInfo()
    const tracker = findPropertyMemoryTracker(noteInfo, propertyKey)
    if (!tracker) {
      return true
    }

    const confirmed = await popups.confirm(
      `Property "${propertyKey}" has a memory tracker. Deleting it will also delete that tracker. Continue?`
    )
    if (!confirmed) {
      return false
    }

    const { error } = await MemoryTrackerController.softDelete({
      path: { memoryTracker: tracker.id },
    })
    if (error) {
      return false
    }

    invalidateNoteInfoCache()
    return true
  }

  const confirmAndApplyRename = async (
    fromKey: string,
    toKey: string
  ): Promise<boolean> => {
    if (noteId() === undefined) {
      return true
    }

    const noteInfo = await loadNoteInfo()
    const tracker = findPropertyMemoryTracker(noteInfo, fromKey)
    if (!tracker) {
      return true
    }

    const confirmed = await popups.confirm(
      `Property "${fromKey}" has a memory tracker. Renaming it to "${toKey}" will update the tracker. Continue?`
    )
    if (!confirmed) {
      return false
    }

    const { error } = await MemoryTrackerController.updatePropertyKey({
      path: { memoryTracker: tracker.id },
      body: { propertyKey: toKey },
    })
    if (error) {
      await popups.alert(
        toOpenApiError(error).message ?? "Failed to update memory tracker"
      )
      return false
    }

    invalidateNoteInfoCache()
    return true
  }

  const confirmAndApplyPropertyKeyChanges = async (
    changes: PropertyKeyChange[]
  ): Promise<boolean> => {
    const renames = changes.filter((change) => change.type === "rename")
    const removals = changes.filter((change) => change.type === "removal")

    for (const change of renames) {
      const proceed = await confirmAndApplyRename(change.fromKey, change.toKey)
      if (!proceed) {
        return false
      }
    }

    for (const change of removals) {
      const proceed = await confirmAndApplyRemoval(change.key)
      if (!proceed) {
        return false
      }
    }

    return true
  }

  return {
    confirmAndApplyRemoval,
    confirmAndApplyRename,
    confirmAndApplyPropertyKeyChanges,
  }
}
