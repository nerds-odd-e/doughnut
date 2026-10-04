import {
  MemoryTrackerController,
  NoteController,
} from "@generated/donut-backend-api/sdk.gen"
import { usePropertyMemoryTrackerGuard } from "@/composables/usePropertyMemoryTrackerGuard"
import makeMe from "donut-test-fixtures/makeMe"
import { mockSdkService, wrapSdkError, wrapSdkResponse } from "@tests/helpers"
import {
  answerOnlyPendingPopup,
  pendingPopups,
} from "@tests/helpers/popupStackTestSupport"
import { afterEach, beforeEach, describe, expect, it, vi } from "vitest"

describe("usePropertyMemoryTrackerGuard", () => {
  const noteId = 42
  let getNoteInfoSpy: ReturnType<typeof mockSdkService>
  let deleteSpy: ReturnType<typeof mockSdkService>
  let updatePropertyKeySpy: ReturnType<typeof mockSdkService>

  beforeEach(() => {
    getNoteInfoSpy = mockSdkService(NoteController, "getNoteInfo", {
      memoryTrackers: [],
    })
    deleteSpy = mockSdkService(MemoryTrackerController, "delete", undefined)
    updatePropertyKeySpy = mockSdkService(
      MemoryTrackerController,
      "updatePropertyKey",
      undefined
    )
  })

  afterEach(() => {
    vi.restoreAllMocks()
  })

  function mockNoteInfoWithPropertyTrackers(
    trackers: Array<{ key: string; id: number }>
  ) {
    const memoryTrackers = trackers.map(({ key, id }) =>
      makeMe.aMemoryTracker.id(id).withPropertyKey(key).please()
    )
    getNoteInfoSpy.mockResolvedValue(
      wrapSdkResponse(
        makeMe.aNoteRecallInfo.memoryTrackers(memoryTrackers).please()
      )
    )
    return memoryTrackers
  }

  it("returns true immediately when noteId is undefined", async () => {
    const { confirmAndApplyRemoval } = usePropertyMemoryTrackerGuard(
      () => undefined
    )

    await expect(confirmAndApplyRemoval("topic")).resolves.toBe(true)

    expect(getNoteInfoSpy).not.toHaveBeenCalled()
    expect(pendingPopups()).toEqual([])
  })

  it("returns true without confirm when no matching tracker exists", async () => {
    const { confirmAndApplyRemoval } = usePropertyMemoryTrackerGuard(
      () => noteId
    )

    await expect(confirmAndApplyRemoval("topic")).resolves.toBe(true)

    expect(getNoteInfoSpy).toHaveBeenCalledWith({
      path: { note: noteId },
    })
    expect(pendingPopups()).toEqual([])
  })

  it("hard-deletes the tracker when the user confirms removal", async () => {
    mockNoteInfoWithPropertyTrackers([{ key: "topic", id: 99 }])

    const { confirmAndApplyRemoval } = usePropertyMemoryTrackerGuard(
      () => noteId
    )

    const removal = confirmAndApplyRemoval("topic")
    const confirmation = await answerOnlyPendingPopup(true)
    await expect(removal).resolves.toBe(true)

    expect(confirmation).toMatchObject({
      type: "confirm",
      message:
        'Property "topic" has a memory tracker. Deleting it will also delete that tracker. Continue?',
    })
    expect(deleteSpy).toHaveBeenCalledWith({
      path: { memoryTracker: 99 },
    })
  })

  it("returns false when the user cancels the confirm dialog", async () => {
    mockNoteInfoWithPropertyTrackers([{ key: "topic", id: 99 }])

    const { confirmAndApplyRemoval } = usePropertyMemoryTrackerGuard(
      () => noteId
    )

    const removal = confirmAndApplyRemoval("topic")
    await answerOnlyPendingPopup(false)
    await expect(removal).resolves.toBe(false)

    expect(deleteSpy).not.toHaveBeenCalled()
  })

  it("returns false when delete fails", async () => {
    mockNoteInfoWithPropertyTrackers([{ key: "topic", id: 99 }])
    deleteSpy.mockResolvedValue(wrapSdkError("server error"))

    const { confirmAndApplyRemoval } = usePropertyMemoryTrackerGuard(
      () => noteId
    )

    const removal = confirmAndApplyRemoval("topic")
    await answerOnlyPendingPopup(true)
    await expect(removal).resolves.toBe(false)
  })

  it("updates the tracker property key when the user confirms a rename", async () => {
    mockNoteInfoWithPropertyTrackers([{ key: "topic", id: 99 }])

    const { confirmAndApplyRename } = usePropertyMemoryTrackerGuard(
      () => noteId
    )

    const rename = confirmAndApplyRename("topic", "subject")
    const confirmation = await answerOnlyPendingPopup(true)
    await expect(rename).resolves.toBe(true)

    expect(confirmation).toMatchObject({
      type: "confirm",
      message:
        'Property "topic" has a memory tracker. Renaming it to "subject" will update the tracker. Continue?',
    })
    expect(updatePropertyKeySpy).toHaveBeenCalledWith({
      path: { memoryTracker: 99 },
      body: { propertyKey: "subject" },
    })
  })

  it("returns false when the user cancels the rename confirm dialog", async () => {
    mockNoteInfoWithPropertyTrackers([{ key: "topic", id: 99 }])

    const { confirmAndApplyRename } = usePropertyMemoryTrackerGuard(
      () => noteId
    )

    const rename = confirmAndApplyRename("topic", "subject")
    await answerOnlyPendingPopup(false)
    await expect(rename).resolves.toBe(false)

    expect(updatePropertyKeySpy).not.toHaveBeenCalled()
  })

  it("shows an alert and returns false when updatePropertyKey fails", async () => {
    mockNoteInfoWithPropertyTrackers([{ key: "topic", id: 99 }])
    updatePropertyKeySpy.mockResolvedValue(wrapSdkError("server error"))

    const { confirmAndApplyRename } = usePropertyMemoryTrackerGuard(
      () => noteId
    )

    const rename = confirmAndApplyRename("topic", "subject")
    await answerOnlyPendingPopup(true)
    const alert = await answerOnlyPendingPopup(true)
    await expect(rename).resolves.toBe(false)

    expect(alert).toMatchObject({ type: "alert", message: "server error" })
  })

  it("processes renames before removals in confirmAndApplyPropertyKeyChanges", async () => {
    mockNoteInfoWithPropertyTrackers([
      { key: "topic", id: 99 },
      { key: "old", id: 100 },
    ])

    const { confirmAndApplyPropertyKeyChanges } = usePropertyMemoryTrackerGuard(
      () => noteId
    )

    const changes = confirmAndApplyPropertyKeyChanges([
      { type: "removal", key: "old" },
      { type: "rename", fromKey: "topic", toKey: "subject" },
    ])
    await answerOnlyPendingPopup(true)
    await answerOnlyPendingPopup(true)
    await expect(changes).resolves.toBe(true)

    expect(updatePropertyKeySpy).toHaveBeenCalledBefore(deleteSpy)
    expect(updatePropertyKeySpy).toHaveBeenCalledWith({
      path: { memoryTracker: 99 },
      body: { propertyKey: "subject" },
    })
    expect(deleteSpy).toHaveBeenCalledWith({
      path: { memoryTracker: 100 },
    })
  })
})
