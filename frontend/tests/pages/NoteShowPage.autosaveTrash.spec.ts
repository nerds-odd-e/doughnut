import {
  NoteController,
  RelationController,
  TextContentController,
} from "@generated/donut-backend-api/sdk.gen"
import usePopups from "@/components/commons/Popups/usePopups"
import { teardownGlobalClientForTesting } from "@/managedApi/clientSetup"
import { cleanup } from "@testing-library/vue"
import { config, flushPromises } from "@vue/test-utils"
import {
  mockNotebookGetForNoteRealm,
  mockSdkService,
  mockSdkServiceWithImplementation,
  productionRouterAt,
  wrapSdkError,
} from "@tests/helpers"
import {
  installMockResizeObserver,
  restoreNoteToolbarWidthMocks,
} from "@tests/helpers/mockNoteToolbarNavWidth"
import {
  advanceNoteContentSaveDebounce,
  deferred,
} from "@tests/helpers/noteContentDebounceTestSupport"
import { noteShowLocation } from "@/routes/noteShowLocation"
import { normalizeNoteContent } from "@/utils/normalizeNoteContent"
import makeMe from "donut-test-fixtures/makeMe"
import { afterEach, beforeEach, describe, expect, it, vi } from "vitest"
import { renderNoteShowPageWithoutSidebar } from "./noteShowPageTestSupport"
import { qualifyingRelationRealmForTrash } from "../notes/noteMoreOptionsTrashTestSupport"

async function editBody(content: string) {
  const editButton = document.querySelector(
    'button[title="Edit as markdown (m)"]'
  ) as HTMLButtonElement
  editButton.click()
  await flushPromises()

  const textarea = document.querySelector(
    '[aria-label="Note content"] textarea'
  ) as HTMLTextAreaElement
  setBodyValue(textarea, content)
  await flushPromises()
  return textarea
}

function setBodyValue(textarea: HTMLTextAreaElement, content: string) {
  textarea.value = content
  textarea.dispatchEvent(new Event("input", { bubbles: true }))
}

async function startTrash(choice: boolean | string) {
  const trashButton = document.querySelector(
    'button[title="Trash note (d)"]'
  ) as HTMLButtonElement
  trashButton.click()
  await flushPromises()
  usePopups().popups.done(choice)
  await flushPromises()
}

describe("note show autosave before trashing", () => {
  const previousErrorHandler = config.global.config.errorHandler
  const onError = vi.fn()
  beforeEach(() => {
    onError.mockClear()
    config.global.config.errorHandler = onError
    vi.useFakeTimers()
    installMockResizeObserver()
  })

  afterEach(() => {
    cleanup()
    config.global.config.errorHandler = previousErrorHandler
    document.body.innerHTML = ""
    vi.useRealTimers()
    vi.restoreAllMocks()
    restoreNoteToolbarWidthMocks()
    teardownGlobalClientForTesting()
  })

  it("reopens mutations after trash failure and skips trashing after save failure", async () => {
    const { relationRealm } = qualifyingRelationRealmForTrash()
    const router = await productionRouterAt(noteShowLocation(relationRealm.id))
    mockSdkService(NoteController, "showNote", relationRealm)
    mockNotebookGetForNoteRealm(relationRealm)
    const firstSave = deferred()
    const secondSave = deferred()
    const mutationOrder: string[] = []
    let saveCalls = 0
    const updateSpy = mockSdkServiceWithImplementation(
      TextContentController,
      "updateNoteContent",
      async ({ body }) => {
        saveCalls += 1
        mutationOrder.push(`save-${saveCalls}-start`)
        if (saveCalls === 1) await firstSave.promise
        if (saveCalls === 2) await secondSave.promise
        mutationOrder.push(`save-${saveCalls}-finish`)
        return makeMe.aNoteRealm
          .id(relationRealm.id)
          .content(body?.content ?? "")
          .please()
      }
    )
    const trashSpy = mockSdkService(
      RelationController,
      "reduceToSourceProperty",
      relationRealm
    )
    trashSpy.mockImplementation(async () => {
      mutationOrder.push("trash")
      return wrapSdkError("trash failed")
    })

    const originalContent = relationRealm.note.content ?? ""
    const editedRelationship = `${originalContent}Edited relationship`
    await renderNoteShowPageWithoutSidebar(router, relationRealm.id)
    const textarea = await editBody(editedRelationship)
    await advanceNoteContentSaveDebounce()

    expect(mutationOrder).toEqual(["save-1-start"])

    setBodyValue(textarea, originalContent)
    await flushPromises()
    await startTrash("REDUCE")

    expect(mutationOrder).toEqual(["save-1-start"])
    expect(trashSpy).not.toHaveBeenCalled()

    firstSave.resolve()
    await flushPromises()

    expect(mutationOrder).toEqual([
      "save-1-start",
      "save-1-finish",
      "save-2-start",
    ])
    expect(trashSpy).not.toHaveBeenCalled()

    secondSave.resolve()
    await flushPromises()

    expect(mutationOrder).toEqual([
      "save-1-start",
      "save-1-finish",
      "save-2-start",
      "save-2-finish",
      "trash",
    ])
    expect(onError).toHaveBeenCalledWith(
      new Error("trash failed"),
      expect.anything(),
      "native event handler"
    )
    expect(updateSpy).toHaveBeenNthCalledWith(1, {
      path: { note: relationRealm.id },
      body: { content: editedRelationship },
    })
    expect(updateSpy).toHaveBeenNthCalledWith(2, {
      path: { note: relationRealm.id },
      body: { content: normalizeNoteContent(originalContent) },
    })
    vi.runAllTimers()
    await flushPromises()
    expect(updateSpy).toHaveBeenCalledTimes(2)

    updateSpy.mockResolvedValueOnce(wrapSdkError("save failed"))
    setBodyValue(textarea, "Second edit")
    await startTrash("REDUCE")

    expect(updateSpy).toHaveBeenCalledTimes(3)
    expect(trashSpy).toHaveBeenCalledTimes(1)
  })
})
