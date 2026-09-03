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
import { qualifyingRelationRealmForDelete } from "../notes/noteMoreOptionsDeleteTestSupport"

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

async function startDelete(choice: boolean | string) {
  const deleteButton = document.querySelector(
    'button[title="Delete note (d)"]'
  ) as HTMLButtonElement
  deleteButton.click()
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

  it("reopens mutations after delete failure and skips deletion after save failure", async () => {
    const { relationRealm } = qualifyingRelationRealmForDelete()
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
    const deleteSpy = mockSdkService(NoteController, "deleteNote", [])
    deleteSpy.mockImplementation(async () => {
      mutationOrder.push("delete")
      return wrapSdkError("delete failed")
    })

    const editedRelationship = `${relationRealm.note.content}Edited relationship`
    await renderNoteShowPageWithoutSidebar(router, relationRealm.id)
    const textarea = await editBody(editedRelationship)
    await startDelete("REDUCE_TO_SOURCE_PROPERTY")

    expect(mutationOrder).toEqual(["save-1-start"])
    expect(deleteSpy).not.toHaveBeenCalled()

    firstSave.resolve()
    await flushPromises()

    expect(mutationOrder).toEqual(["save-1-start", "save-1-finish", "delete"])
    vi.runAllTimers()
    await flushPromises()
    expect(updateSpy).toHaveBeenCalledTimes(1)

    updateSpy.mockResolvedValueOnce(wrapSdkError("save failed"))
    setBodyValue(textarea, "Second edit")
    await startDelete("REDUCE_TO_SOURCE_PROPERTY")

    expect(updateSpy).toHaveBeenCalledTimes(2)
    expect(deleteSpy).toHaveBeenCalledTimes(1)
  })
})
