import {
  MemoryTrackerController,
  NoteController,
} from "@generated/doughnut-backend-api/sdk.gen"
import { usePropertyMemoryTrackerGuard } from "@/composables/usePropertyMemoryTrackerGuard"
import makeMe from "doughnut-test-fixtures/makeMe"
import { mockSdkService, wrapSdkError, wrapSdkResponse } from "@tests/helpers"
import { afterEach, beforeEach, describe, expect, it, vi } from "vitest"

const confirmMock = vi.fn()
const alertMock = vi.fn()

vi.mock("@/components/commons/Popups/usePopups", () => ({
  default: () => ({
    popups: {
      confirm: confirmMock,
      alert: alertMock,
      options: vi.fn(),
      done: vi.fn(),
      register: vi.fn(),
      peek: vi.fn(),
    },
  }),
}))

describe("usePropertyMemoryTrackerGuard", () => {
  const noteId = 42
  let getNoteInfoSpy: ReturnType<typeof mockSdkService>
  let softDeleteSpy: ReturnType<typeof mockSdkService>
  let updatePropertyKeySpy: ReturnType<typeof mockSdkService>

  beforeEach(() => {
    getNoteInfoSpy = mockSdkService(NoteController, "getNoteInfo", {
      memoryTrackers: [],
    })
    softDeleteSpy = mockSdkService(
      MemoryTrackerController,
      "softDelete",
      undefined
    )
    updatePropertyKeySpy = mockSdkService(
      MemoryTrackerController,
      "updatePropertyKey",
      undefined
    )
    confirmMock.mockReset()
    alertMock.mockReset()
  })

  afterEach(() => {
    vi.restoreAllMocks()
  })

  function mockNoteInfoWithPropertyTracker(key: string, id: number) {
    const tracker = makeMe.aMemoryTracker.please()
    tracker.id = id
    tracker.propertyKey = key
    getNoteInfoSpy.mockResolvedValue(
      wrapSdkResponse(makeMe.aNoteRecallInfo.memoryTrackers([tracker]).please())
    )
    return tracker
  }

  function mockNoteInfoWithPropertyTrackers(
    trackers: Array<{ key: string; id: number }>
  ) {
    const memoryTrackers = trackers.map(({ key, id }) => {
      const tracker = makeMe.aMemoryTracker.please()
      tracker.id = id
      tracker.propertyKey = key
      return tracker
    })
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
    expect(confirmMock).not.toHaveBeenCalled()
    expect(softDeleteSpy).not.toHaveBeenCalled()
  })

  it("returns true without confirm when no matching tracker exists", async () => {
    const { confirmAndApplyRemoval } = usePropertyMemoryTrackerGuard(
      () => noteId
    )

    await expect(confirmAndApplyRemoval("topic")).resolves.toBe(true)

    expect(getNoteInfoSpy).toHaveBeenCalledWith({
      path: { note: noteId },
    })
    expect(confirmMock).not.toHaveBeenCalled()
    expect(softDeleteSpy).not.toHaveBeenCalled()
  })

  it("soft-deletes the tracker when the user confirms removal", async () => {
    const tracker = mockNoteInfoWithPropertyTracker("topic", 99)
    confirmMock.mockImplementationOnce(() => Promise.resolve(true))

    const { confirmAndApplyRemoval } = usePropertyMemoryTrackerGuard(
      () => noteId
    )

    await expect(confirmAndApplyRemoval("topic")).resolves.toBe(true)

    expect(confirmMock).toHaveBeenCalledWith(
      'Property "topic" has a memory tracker. Deleting it will also delete that tracker. Continue?'
    )
    expect(softDeleteSpy).toHaveBeenCalledWith({
      path: { memoryTracker: tracker.id },
    })
  })

  it("returns false when the user cancels the confirm dialog", async () => {
    mockNoteInfoWithPropertyTracker("topic", 99)
    confirmMock.mockImplementationOnce(() => Promise.resolve(false))

    const { confirmAndApplyRemoval } = usePropertyMemoryTrackerGuard(
      () => noteId
    )

    await expect(confirmAndApplyRemoval("topic")).resolves.toBe(false)

    expect(confirmMock).toHaveBeenCalledOnce()
    expect(softDeleteSpy).not.toHaveBeenCalled()
  })

  it("returns false when softDelete fails", async () => {
    const tracker = mockNoteInfoWithPropertyTracker("topic", 99)
    confirmMock.mockImplementationOnce(() => Promise.resolve(true))
    softDeleteSpy.mockResolvedValue(wrapSdkError("server error"))

    const { confirmAndApplyRemoval } = usePropertyMemoryTrackerGuard(
      () => noteId
    )

    await expect(confirmAndApplyRemoval("topic")).resolves.toBe(false)

    expect(softDeleteSpy).toHaveBeenCalledWith({
      path: { memoryTracker: tracker.id },
    })
  })

  it("updates the tracker property key when the user confirms a rename", async () => {
    const tracker = mockNoteInfoWithPropertyTracker("topic", 99)
    confirmMock.mockImplementationOnce(() => Promise.resolve(true))

    const { confirmAndApplyRename } = usePropertyMemoryTrackerGuard(
      () => noteId
    )

    await expect(confirmAndApplyRename("topic", "subject")).resolves.toBe(true)

    expect(confirmMock).toHaveBeenCalledWith(
      'Property "topic" has a memory tracker. Renaming it to "subject" will update the tracker. Continue?'
    )
    expect(updatePropertyKeySpy).toHaveBeenCalledWith({
      path: { memoryTracker: tracker.id },
      body: { propertyKey: "subject" },
    })
  })

  it("returns false when the user cancels the rename confirm dialog", async () => {
    mockNoteInfoWithPropertyTracker("topic", 99)
    confirmMock.mockImplementationOnce(() => Promise.resolve(false))

    const { confirmAndApplyRename } = usePropertyMemoryTrackerGuard(
      () => noteId
    )

    await expect(confirmAndApplyRename("topic", "subject")).resolves.toBe(false)

    expect(confirmMock).toHaveBeenCalledOnce()
    expect(updatePropertyKeySpy).not.toHaveBeenCalled()
  })

  it("shows an alert and returns false when updatePropertyKey fails", async () => {
    const tracker = mockNoteInfoWithPropertyTracker("topic", 99)
    confirmMock.mockImplementationOnce(() => Promise.resolve(true))
    updatePropertyKeySpy.mockResolvedValue(wrapSdkError("server error"))

    const { confirmAndApplyRename } = usePropertyMemoryTrackerGuard(
      () => noteId
    )

    await expect(confirmAndApplyRename("topic", "subject")).resolves.toBe(false)

    expect(updatePropertyKeySpy).toHaveBeenCalledWith({
      path: { memoryTracker: tracker.id },
      body: { propertyKey: "subject" },
    })
    expect(alertMock).toHaveBeenCalledWith("server error")
  })

  it("processes renames before removals in confirmAndApplyPropertyKeyChanges", async () => {
    mockNoteInfoWithPropertyTrackers([
      { key: "topic", id: 99 },
      { key: "old", id: 100 },
    ])
    confirmMock
      .mockImplementationOnce(() => Promise.resolve(true))
      .mockImplementationOnce(() => Promise.resolve(true))

    const { confirmAndApplyPropertyKeyChanges } = usePropertyMemoryTrackerGuard(
      () => noteId
    )

    await expect(
      confirmAndApplyPropertyKeyChanges([
        { type: "removal", key: "old" },
        { type: "rename", fromKey: "topic", toKey: "subject" },
      ])
    ).resolves.toBe(true)

    expect(updatePropertyKeySpy).toHaveBeenCalledBefore(softDeleteSpy)
    expect(updatePropertyKeySpy).toHaveBeenCalledWith({
      path: { memoryTracker: 99 },
      body: { propertyKey: "subject" },
    })
    expect(softDeleteSpy).toHaveBeenCalledWith({
      path: { memoryTracker: 100 },
    })
  })
})
