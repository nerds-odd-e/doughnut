import {
  MemoryTrackerController,
  NoteController,
} from "@generated/donut-backend-api/sdk.gen"
import { usePropertyMemoryTrackerGuard } from "@/composables/usePropertyMemoryTrackerGuard"
import makeMe from "donut-test-fixtures/makeMe"
import { mockSdkService } from "@tests/helpers"
import {
  answerOnlyPendingPopup,
  pendingPopups,
} from "@tests/helpers/popupStackTestSupport"
import { afterEach, beforeEach, describe, expect, it, vi } from "vitest"

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
  })

  afterEach(() => {
    vi.restoreAllMocks()
  })

  const guard = () => usePropertyMemoryTrackerGuard(() => noteId)

  /** Confirms the one prompt the action raises, then waits for its result. */
  async function confirmingOnce<T>(action: Promise<T>) {
    expect(await answerOnlyPendingPopup(true)).toMatchObject({
      type: "confirm",
    })
    const result = await action
    expect(pendingPopups()).toEqual([])
    return result
  }

  it("renames every value's tracker after one confirmation", async () => {
    await expect(
      confirmingOnce(guard().confirmAndApplyRename("example of", "sample of"))
    ).resolves.toBe(true)

    expect(updatePropertyKeySpy.mock.calls.map(([options]) => options)).toEqual(
      [
        { path: { memoryTracker: 1 }, body: { propertyKey: "sample of" } },
        { path: { memoryTracker: 2 }, body: { propertyKey: "sample of" } },
      ]
    )
  })

  it("deletes every value's tracker after one confirmation", async () => {
    await expect(
      confirmingOnce(guard().confirmAndApplyRemoval("example of"))
    ).resolves.toBe(true)

    expect(deleteSpy.mock.calls.map(([options]) => options)).toEqual([
      { path: { memoryTracker: 1 } },
      { path: { memoryTracker: 2 } },
    ])
  })

  it("carries every value's tracker for Markdown key changes", async () => {
    await expect(
      confirmingOnce(
        guard().confirmAndApplyPropertyKeyChanges([
          { type: "rename", fromKey: "example of", toKey: "sample of" },
        ])
      )
    ).resolves.toBe(true)

    expect(updatePropertyKeySpy).toHaveBeenCalledTimes(2)
  })
})
