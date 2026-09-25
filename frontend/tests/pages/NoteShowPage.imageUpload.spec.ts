import {
  NoteController,
  TextContentController,
} from "@generated/donut-backend-api/sdk.gen"
import { teardownGlobalClientForTesting } from "@/managedApi/clientSetup"
import { cleanup } from "@testing-library/vue"
import { flushPromises } from "@vue/test-utils"
import {
  mockNotebookGetForNoteRealm,
  mockSdkService,
  mockSdkServiceWithImplementation,
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

const noteRealm = makeMe.aNoteRealm
  .content("---\nimage: old.png\n---\n\nHi")
  .please()
const editedContent = "---\nimage: old.png\n---\n\nHi there"
const uploadedContent = "---\ntype: Note\nimage: e2e.png\n---\n\nHi there"

function clickButtonTitled(title: string) {
  ;(document.querySelector(`button[title="${title}"]`) as HTMLElement).click()
}

async function editBodyAsMarkdownThenReturnToRich(content: string) {
  clickButtonTitled("Edit as markdown (m)")
  await flushPromises()
  const textarea = document.querySelector(
    '[aria-label="Note content"] textarea'
  ) as HTMLTextAreaElement
  textarea.value = content
  textarea.dispatchEvent(new Event("input", { bubbles: true }))
  await flushPromises()
  clickButtonTitled("Edit as rich content (m)")
  await flushPromises()
}

async function choosePicture() {
  const input = document.querySelector(
    '[data-testid="rich-note-image-property-file-input"]'
  ) as HTMLInputElement
  Object.defineProperty(input, "files", {
    value: [new File(["fake"], "e2e.png", { type: "image/png" })],
    configurable: true,
  })
  input.dispatchEvent(new Event("change"))
  await flushPromises()
}

describe("note show picture upload", () => {
  const requests: string[] = []
  let saveSpy: ReturnType<typeof mockSdkServiceWithImplementation>

  beforeEach(() => {
    vi.useFakeTimers()
    installMockResizeObserver()
    requests.length = 0
    mockSdkService(NoteController, "showNote", noteRealm)
    mockNotebookGetForNoteRealm(noteRealm)
    saveSpy = mockSdkServiceWithImplementation(
      TextContentController,
      "updateNoteContent",
      async ({ body }) => {
        requests.push(`save: ${body?.content}`)
        return makeMe.aNoteRealm
          .id(noteRealm.id)
          .content(body?.content ?? "")
          .please()
      }
    )
    mockSdkServiceWithImplementation(
      NoteController,
      "uploadNoteImage",
      async () => {
        requests.push("upload")
        return makeMe.aNoteRealm
          .id(noteRealm.id)
          .content(uploadedContent)
          .please()
      }
    )
  })

  afterEach(() => {
    cleanup()
    document.body.innerHTML = ""
    vi.useRealTimers()
    vi.restoreAllMocks()
    restoreNoteToolbarWidthMocks()
    teardownGlobalClientForTesting()
  })

  it("saves pending text before the upload request", async () => {
    await renderNoteShowPageWithoutSidebar(
      createNoteShowPageRouter(),
      noteRealm.id
    )
    await editBodyAsMarkdownThenReturnToRich(editedContent)

    await choosePicture()

    expect(requests).toEqual([`save: ${editedContent}`, "upload"])
  })

  it("shows the uploaded note without saving its content again", async () => {
    await renderNoteShowPageWithoutSidebar(
      createNoteShowPageRouter(),
      noteRealm.id
    )

    await choosePicture()
    vi.runAllTimers()
    await flushPromises()

    const imageValue = document.querySelector(
      '[data-testid="rich-note-property-row-value-input"]'
    ) as HTMLInputElement
    expect(imageValue.value).toBe("e2e.png")
    expect(saveSpy).not.toHaveBeenCalled()
  })
})
