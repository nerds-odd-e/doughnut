import {
  MemoryTrackerController,
  NoteController,
} from "@generated/donut-backend-api/sdk.gen"
import { usePropertyMemoryTrackerGuard } from "@/composables/usePropertyMemoryTrackerGuard"
import makeMe from "donut-test-fixtures/makeMe"
import { mockSdkService } from "@tests/helpers"
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

describe("usePropertyMemoryTrackerGuard on a list key with tracked values", () => {
  const noteId = 42
  let deleteSpy: ReturnType<typeof mockSdkService>
  let updatePropertyKeySpy: ReturnType<typeof mockSdkService>

  beforeEach(() => {
    mockSdkService(NoteController, "getNoteInfo", {
      memoryTrackers: [
        makeMe.aMemoryTracker.id(1).withPropertyKey("example of", "[[run]]"),
        makeMe.aMemoryTracker
          .id(2)
          .withPropertyKey("example of", "[[past tense]]"),
        makeMe.aMemoryTracker.id(3).withPropertyKey("topic"),
      ].map((builder) => builder.please()),
    })
    deleteSpy = mockSdkService(MemoryTrackerController, "delete", undefined)
    updatePropertyKeySpy = mockSdkService(
      MemoryTrackerController,
      "updatePropertyKey",
      undefined
    )
    confirmMock.mockReset()
    confirmMock.mockResolvedValue(true)
  })

  afterEach(() => {
    vi.restoreAllMocks()
  })

  const guard = () => usePropertyMemoryTrackerGuard(() => noteId)

  it("renames every value's tracker after one confirmation", async () => {
    await expect(
      guard().confirmAndApplyRename("example of", "sample of")
    ).resolves.toBe(true)

    expect(confirmMock).toHaveBeenCalledOnce()
    expect(updatePropertyKeySpy.mock.calls.map(([options]) => options)).toEqual(
      [
        { path: { memoryTracker: 1 }, body: { propertyKey: "sample of" } },
        { path: { memoryTracker: 2 }, body: { propertyKey: "sample of" } },
      ]
    )
  })

  it("deletes every value's tracker after one confirmation", async () => {
    await expect(guard().confirmAndApplyRemoval("example of")).resolves.toBe(
      true
    )

    expect(confirmMock).toHaveBeenCalledOnce()
    expect(deleteSpy.mock.calls.map(([options]) => options)).toEqual([
      { path: { memoryTracker: 1 } },
      { path: { memoryTracker: 2 } },
    ])
  })

  it("carries every value's tracker for Markdown key changes", async () => {
    await expect(
      guard().confirmAndApplyPropertyKeyChanges([
        { type: "rename", fromKey: "example of", toKey: "sample of" },
      ])
    ).resolves.toBe(true)

    expect(confirmMock).toHaveBeenCalledOnce()
    expect(updatePropertyKeySpy).toHaveBeenCalledTimes(2)
  })
})
