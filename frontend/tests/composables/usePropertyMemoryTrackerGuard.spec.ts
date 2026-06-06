import {
  MemoryTrackerController,
  NoteController,
} from "@generated/doughnut-backend-api/sdk.gen"
import { usePropertyMemoryTrackerGuard } from "@/composables/usePropertyMemoryTrackerGuard"
import makeMe from "doughnut-test-fixtures/makeMe"
import { mockSdkService, wrapSdkError, wrapSdkResponse } from "@tests/helpers"
import { afterEach, beforeEach, describe, expect, it, vi } from "vitest"

const confirmMock = vi.fn()

vi.mock("@/components/commons/Popups/usePopups", () => ({
  default: () => ({
    popups: {
      confirm: confirmMock,
      alert: vi.fn(),
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

  beforeEach(() => {
    getNoteInfoSpy = mockSdkService(NoteController, "getNoteInfo", {
      memoryTrackers: [],
    })
    softDeleteSpy = mockSdkService(
      MemoryTrackerController,
      "softDelete",
      undefined
    )
    confirmMock.mockReset()
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
})
