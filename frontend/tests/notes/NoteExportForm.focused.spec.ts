import { afterEach, beforeEach, describe, expect, it, vi } from "vitest"
import { flushPromises, type VueWrapper } from "@vue/test-utils"
import { saveAs } from "file-saver"
import makeMe from "donut-test-fixtures/makeMe"
import { NoteExportController } from "@generated/donut-backend-api/sdk.gen"
import {
  mockSdkService,
  productionRouterAt,
  wrapSdkResponse,
} from "@tests/helpers"
import {
  notePropertyItemLocation,
  notePropertyLocation,
} from "@/routes/noteShowLocation"
import {
  mountNoteToolbar,
  noteToolbarAction,
  openNoteToolbarOverflowMenu,
  overflowMenuItem,
  resetNoteToolbarTestState,
} from "./noteToolbarTestHelpers"
import {
  allMoreOptionsFitNavWidth,
  exportOverflowNavWidth,
  installMockResizeObserver,
  layoutNoteToolbar,
  restoreNoteToolbarWidthMocks,
} from "@tests/helpers/mockNoteToolbarNavWidth"
import {
  aiMarkdownTextarea,
  copyButton,
  downloadButton,
} from "./noteExportFormTestSupport"
import { noteMoreOptionsTitles } from "@/components/notes/widgets/noteMoreOptionsTitles"
import { notebookSidebarClosedPlugin } from "@tests/helpers/notebookSidebarTestProvide"
import type { RouteLocationRaw } from "vue-router"

vi.mock("file-saver", () => ({ saveAs: vi.fn() }))
vi.mock("@/models/audio/recorderWorklet", async () =>
  (await import("./noteVoiceInputButtonMocks")).recorderWorkletMockExports()
)
vi.mock("@/models/audio/audioRecorder", async () =>
  (await import("./noteVoiceInputButtonMocks")).audioRecorderMockExports()
)
vi.mock("@/models/wakeLocker", async () =>
  (await import("./noteVoiceInputButtonMocks")).wakeLockerMockExports()
)

const noteRealm = makeMe.aNoteRealm
  .content(
    '---\ntopic: Motion\nexample of: ["[[run]]", "[[run]]", "[[walk]]"]\n---\nSurrounding explanation'
  )
  .please()
const noteId = noteRealm.note.id
const item = { value: "[[run]]", occurrence: 2 }
const itemLocation = () => notePropertyItemLocation(noteId, "example of", item)
const focusedMarkdown =
  "Property value: [[run]]\nHighlighted item occurrence: 2\nSurrounding explanation"
const scalarMarkdown = "Property key: topic\nProperty value: Motion"
const wholeListMarkdown =
  "Property key: example of\nFocus: the whole list property, across all its values."
const questionsMarkdown = `${focusedMarkdown}\n## Recent multiple-choice questions\nWhich activity?`

describe("exporting the note toolbar's highlight", () => {
  let wrapper: VueWrapper
  let writeText: ReturnType<typeof vi.fn>
  beforeEach(() => {
    installMockResizeObserver()
    resetNoteToolbarTestState()
    vi.clearAllMocks()
    writeText = vi.fn().mockResolvedValue(undefined)
    vi.stubGlobal("navigator", { ...navigator, clipboard: { writeText } })
    mockSdkService(NoteExportController, "getAiContextMarkdown", {
      markdown: focusedMarkdown,
    })
  })
  afterEach(() => {
    wrapper?.unmount()
    document.body.innerHTML = ""
    restoreNoteToolbarWidthMocks()
    vi.unstubAllGlobals()
  })
  async function mountToolbar(location: RouteLocationRaw, overflow = false) {
    const router = await productionRouterAt(location)
    wrapper = await mountNoteToolbar(noteRealm, {
      router,
      plugin: notebookSidebarClosedPlugin(),
    })
    await layoutNoteToolbar(
      wrapper,
      overflow ? exportOverflowNavWidth() : allMoreOptionsFitNavWidth()
    )
  }
  async function openExport(overflow = false) {
    if (overflow) {
      await openNoteToolbarOverflowMenu(wrapper)
      overflowMenuItem(noteMoreOptionsTitles.export)!.click()
    } else {
      await noteToolbarAction(wrapper, noteMoreOptionsTitles.export).trigger(
        "click"
      )
    }
    await flushPromises()
  }
  const checkbox = () =>
    document.querySelector<HTMLInputElement>(
      '[data-testid="include-recent-questions-checkbox"]'
    )!
  async function includeQuestions(checked: boolean) {
    checkbox().checked = checked
    checkbox().dispatchEvent(new Event("change", { bubbles: true }))
    await flushPromises()
  }
  it.each([
    {
      location: notePropertyLocation(noteId, "topic"),
      query: { propertyKey: "topic" },
      markdown: scalarMarkdown,
      overflow: false,
    },
    {
      location: notePropertyLocation(noteId, "example of"),
      query: { propertyKey: "example of" },
      markdown: wholeListMarkdown,
      overflow: true,
    },
    {
      location: itemLocation(),
      query: {
        propertyKey: "example of",
        propertyValue: item.value,
        propertyOccurrence: 2,
      },
      markdown: focusedMarkdown,
      overflow: false,
    },
    {
      location: itemLocation(),
      query: {
        propertyKey: "example of",
        propertyValue: item.value,
        propertyOccurrence: 2,
      },
      markdown: focusedMarkdown,
      overflow: true,
    },
  ])(
    "opens the visible focus from $location.name (overflow=$overflow)",
    async ({ location, query, markdown, overflow }) => {
      await mountToolbar(location, overflow)
      vi.mocked(
        NoteExportController.getAiContextMarkdown
      ).mockResolvedValueOnce(wrapSdkResponse({ markdown }))
      await openExport(overflow)
      expect(
        NoteExportController.getAiContextMarkdown
      ).toHaveBeenLastCalledWith({
        path: { note: noteId },
        query: { tokenLimit: 2000, ...query },
      })
      expect(checkbox().checked).toBe(false)
      expect(aiMarkdownTextarea()?.value).toBe(markdown)
    }
  )

  it("keeps the exact item through budget and checkbox refresh, copy and download", async () => {
    await mountToolbar(itemLocation())
    await openExport()
    const input = document.querySelector<HTMLInputElement>(
      '[data-testid="token-limit-input"]'
    )!
    input.value = "3500"
    input.dispatchEvent(new Event("input", { bubbles: true }))
    document
      .querySelector<HTMLButtonElement>(
        '[data-testid="refresh-context-md-btn"]'
      )!
      .click()
    await flushPromises()
    expect(NoteExportController.getAiContextMarkdown).toHaveBeenLastCalledWith({
      path: { note: noteId },
      query: {
        tokenLimit: 3500,
        propertyKey: "example of",
        propertyValue: item.value,
        propertyOccurrence: 2,
      },
    })
    vi.mocked(NoteExportController.getAiContextMarkdown).mockResolvedValueOnce(
      wrapSdkResponse({ markdown: questionsMarkdown })
    )
    await includeQuestions(true)
    expect(NoteExportController.getAiContextMarkdown).toHaveBeenLastCalledWith({
      path: { note: noteId },
      query: {
        tokenLimit: 3500,
        propertyKey: "example of",
        propertyValue: item.value,
        propertyOccurrence: 2,
        includeRecentQuestions: true,
      },
    })
    copyButton().click()
    await flushPromises()
    downloadButton().click()
    await flushPromises()
    expect(writeText).toHaveBeenLastCalledWith(questionsMarkdown)
    expect(await (vi.mocked(saveAs).mock.calls.at(-1)![0] as Blob).text()).toBe(
      aiMarkdownTextarea()?.value
    )
    await includeQuestions(false)
    expect(aiMarkdownTextarea()?.value).toBe(focusedMarkdown)
    expect(NoteExportController.getAiContextMarkdown).toHaveBeenLastCalledWith({
      path: { note: noteId },
      query: {
        tokenLimit: 3500,
        propertyKey: "example of",
        propertyValue: item.value,
        propertyOccurrence: 2,
      },
    })
  })
})
