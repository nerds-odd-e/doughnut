import type {
  MemoryTracker,
  NoteRecallInfo,
} from "@generated/donut-backend-api"
import {
  MemoryTrackerController,
  NoteController,
} from "@generated/donut-backend-api/sdk.gen"
import usePopups from "@/components/commons/Popups/usePopups"
import { apiCallWithLoading } from "@/managedApi/clientSetup"
import { toOpenApiError } from "@/managedApi/openApiError"
import type {
  PropertyKeyChange,
  PropertyValueFollow,
} from "@/utils/noteContentFrontmatter"

function propertyMemoryTrackers(
  noteInfo: NoteRecallInfo | undefined,
  propertyKey: string
): MemoryTracker[] {
  return (
    noteInfo?.memoryTrackers?.filter(
      (tracker) => tracker.propertyKey && tracker.propertyKey === propertyKey
    ) ?? []
  )
}

export function usePropertyMemoryTrackerGuard(
  noteId: () => number | undefined,
  onMemoryTrackerChanged?: () => void | Promise<void>
) {
  const { popups } = usePopups()
  const notifyChanged = onMemoryTrackerChanged ?? (() => undefined)
  let pendingNoteInfoRead:
    | { noteId: number; promise: Promise<NoteRecallInfo | undefined> }
    | undefined

  const loadNoteInfo = async (): Promise<NoteRecallInfo | undefined> => {
    const id = noteId()
    if (id === undefined) {
      return
    }

    let read = pendingNoteInfoRead
    if (read?.noteId !== id) {
      const promise = apiCallWithLoading(() =>
        NoteController.getNoteInfo({ path: { note: id } })
      ).then(({ data, error }) => (!error ? data : undefined))
      read = { noteId: id, promise }
      pendingNoteInfoRead = read
    }

    try {
      const info = await read.promise
      return noteId() === id ? info : undefined
    } finally {
      if (pendingNoteInfoRead === read) pendingNoteInfoRead = undefined
    }
  }

  const confirmAndApplyRemoval = async (
    propertyKey: string,
    propertyValue?: string
  ): Promise<boolean> => {
    if (noteId() === undefined) {
      return true
    }

    const noteInfo = await loadNoteInfo()
    if (!noteInfo) return false
    const trackers = propertyMemoryTrackers(noteInfo, propertyKey).filter(
      (tracker) =>
        propertyValue === undefined || tracker.propertyValue === propertyValue
    )
    if (trackers.length === 0) {
      return true
    }

    const confirmed = await popups.confirm(
      propertyValue === undefined
        ? `Property "${propertyKey}" has a memory tracker. Deleting it will also delete that tracker. Continue?`
        : `Removing "${propertyValue}" from property "${propertyKey}" will delete its learning history. Equal values share this history; any remaining equal occurrence will lose it too. Continue?`
    )
    if (!confirmed) {
      return false
    }

    for (const tracker of trackers) {
      const { error } = await apiCallWithLoading(() =>
        MemoryTrackerController.delete({
          path: { memoryTracker: tracker.id },
        })
      )
      if (error) {
        return false
      }
    }

    await notifyChanged()
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
    if (!noteInfo) return false
    const trackers = propertyMemoryTrackers(noteInfo, fromKey)
    if (trackers.length === 0) {
      return true
    }

    const confirmed = await popups.confirm(
      `Property "${fromKey}" has a memory tracker. Renaming it to "${toKey}" will update the tracker. Continue?`
    )
    if (!confirmed) {
      return false
    }

    for (const tracker of trackers) {
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
    }

    await notifyChanged()
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

  /** No confirmation: the trackers keep their history on the value they were about. */
  const applyTrackersFollowingValues = async (
    follows: PropertyValueFollow[]
  ) => {
    const id = noteId()
    if (id === undefined || follows.length === 0) {
      return
    }

    for (const { key, value } of follows) {
      await MemoryTrackerController.followPropertyValue({
        path: { note: id },
        body: { propertyKey: key, propertyValue: value },
      })
    }

    await notifyChanged()
  }

  return {
    applyTrackersFollowingValues,
    confirmAndApplyRemoval,
    confirmAndApplyRename,
    confirmAndApplyPropertyKeyChanges,
  }
}
