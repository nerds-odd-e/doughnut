import {
  NoteController,
  TextContentController,
} from "@generated/donut-backend-api/sdk.gen"
import usePopups from "@/components/commons/Popups/usePopups"
import { teardownGlobalClientForTesting } from "@/managedApi/clientSetup"
import { cleanup } from "@testing-library/vue"
import { flushPromises } from "@vue/test-utils"
import {
  mockNotebookGetForNoteRealm,
  mockSdkService,
  mockSdkServiceWithImplementation,
  wrapSdkError,
} from "@tests/helpers"
import {
  installMockResizeObserver,
  restoreNoteToolbarWidthMocks,
} from "@tests/helpers/mockNoteToolbarNavWidth"
import makeMe from "donut-test-fixtures/makeMe"
import { afterEach, beforeEach, describe, expect, it, vi } from "vitest"
import {
  createNoteShowPageRouter,
  renderNoteShowPageWithoutSidebar,
} from "./noteShowPageTestSupport"
import { qualifyingRelationRealmForTrash } from "../notes/noteMoreOptionsTrashTestSupport"

function deferred<T>() {
  let resolve!: (value: T) => void
  const promise = new Promise<T>((done) => {
    resolve = done
  })
  return { promise, resolve }
}

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

describe("note show autosave before deletion", () => {
  beforeEach(() => {
    vi.useFakeTimers()
    installMockResizeObserver()
  })

  afterEach(() => {
    cleanup()
    document.body.innerHTML = ""
    vi.useRealTimers()
    vi.restoreAllMocks()
    restoreNoteToolbarWidthMocks()
    teardownGlobalClientForTesting()
  })

  it("reopens mutations after trash failure and skips trashing after save failure", async () => {
    const { relationRealm } = qualifyingRelationRealmForTrash()
    const router = createNoteShowPageRouter()
    mockSdkService(NoteController, "showNote", relationRealm)
    mockNotebookGetForNoteRealm(relationRealm)
    const firstSave = deferred<void>()
    const mutationOrder: string[] = []
    let saveCalls = 0
    const updateSpy = mockSdkServiceWithImplementation(
      TextContentController,
      "updateNoteContent",
      async ({ body }) => {
        saveCalls += 1
        mutationOrder.push(`save-${saveCalls}-start`)
        if (saveCalls === 1) await firstSave.promise
        mutationOrder.push(`save-${saveCalls}-finish`)
        return makeMe.aNoteRealm
          .id(relationRealm.id)
          .content(body?.content ?? "")
          .please()
      }
    )
    const trashSpy = mockSdkService(NoteController, "trashNote", relationRealm)
    trashSpy.mockImplementation(async () => {
      mutationOrder.push("trash")
      return wrapSdkError("trash failed")
    })

    const editedRelationship = `${relationRealm.note.content}Edited relationship`
    await renderNoteShowPageWithoutSidebar(router, relationRealm.id)
    const textarea = await editBody(editedRelationship)
    await startTrash("REDUCE_TO_SOURCE_PROPERTY")

    expect(mutationOrder).toEqual(["save-1-start"])
    expect(trashSpy).not.toHaveBeenCalled()

    firstSave.resolve()
    await flushPromises()

    expect(mutationOrder).toEqual(["save-1-start", "save-1-finish", "trash"])
    vi.runAllTimers()
    await flushPromises()
    expect(updateSpy).toHaveBeenCalledTimes(1)

    updateSpy.mockResolvedValueOnce(wrapSdkError("save failed"))
    setBodyValue(textarea, "Second edit")
    await startTrash("REDUCE_TO_SOURCE_PROPERTY")

    expect(updateSpy).toHaveBeenCalledTimes(2)
    expect(trashSpy).toHaveBeenCalledTimes(1)
  })
})
