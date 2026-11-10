import {
  describe,
  it,
  vi,
  expect,
  beforeEach,
  afterEach,
  beforeAll,
  afterAll,
} from "vitest"
import helper, {
  mockSdkService,
  wrapSdkError,
  wrapSdkResponse,
} from "@tests/helpers"
import makeMe from "donut-test-fixtures/makeMe"
import NoteExportForm from "@/components/notes/core/NoteExportForm.vue"
import { NoteExportController } from "@generated/donut-backend-api/sdk.gen"
import {
  setupGlobalClient,
  teardownGlobalClientForTesting,
} from "@/managedApi/clientSetup"
import {
  showToastsOnPage,
  toastMessage,
  toastShown,
} from "@tests/helpers/toastTestSupport"
import { saveAs } from "file-saver"
import { flushPromises, type VueWrapper } from "@vue/test-utils"
import type { Note } from "@generated/donut-backend-api"

vi.mock("file-saver", () => ({ saveAs: vi.fn() }))

import {
  aiMarkdownTextarea,
  copyButton,
  downloadButton,
} from "./noteExportFormTestSupport"

const ordinaryMarkdown = "# AI context\n\nHello **world**."
const withQuestionsMarkdown =
  "# AI context\n\nHello **world**.\n\n## Recent multiple-choice questions\n\n### Q1\nStem one"

describe("NoteExportForm", () => {
  let wrapper: VueWrapper
  let writeText: ReturnType<typeof vi.fn>

  showToastsOnPage()
  beforeAll(() => setupGlobalClient({ states: [] }))
  afterAll(teardownGlobalClientForTesting)

  beforeEach(() => {
    vi.clearAllMocks()
    writeText = vi.fn().mockResolvedValue(undefined)
    vi.stubGlobal("navigator", { ...navigator, clipboard: { writeText } })
    mockSdkService(NoteExportController, "getAiContextMarkdown", {
      markdown: ordinaryMarkdown,
    })
  })

  afterEach(() => {
    wrapper?.unmount()
    document.body.innerHTML = ""
    vi.unstubAllGlobals()
  })

  const mountForm = (note: Note = makeMe.aNote.please()) => {
    wrapper = helper
      .component(NoteExportForm)
      .withRouter()
      .withProps({ note })
      .mount({ attachTo: document.body })
    return note
  }

  const inclusionCheckbox = () =>
    wrapper.find('[data-testid="include-recent-questions-checkbox"]')

  const clickTestId = async (testId: string) => {
    await wrapper.find(`[data-testid="${testId}"]`).trigger("click")
    await flushPromises()
  }

  const setTokenLimit = async (value: string) => {
    const input = wrapper.find('[data-testid="token-limit-input"]')
    await input.setValue(value)
    await flushPromises()
  }

  const setInclusion = async (checked: boolean) => {
    const checkbox = inclusionCheckbox()
    ;(checkbox.element as HTMLInputElement).checked = checked
    await checkbox.trigger("change")
    await flushPromises()
  }

  const expectExportActions = (available: boolean) => {
    expect(copyButton().disabled).toBe(!available)
    expect(downloadButton().disabled).toBe(!available)
  }

  const copyAndDownloadText = async () => {
    await clickTestId("copy-ai-context-md-btn")
    expect(writeText).toHaveBeenLastCalledWith(aiMarkdownTextarea()?.value)
    await clickTestId("download-ai-context-md-btn")
    const blobArg = vi.mocked(saveAs).mock.calls.at(-1)![0] as Blob
    expect(await blobArg.text()).toBe(aiMarkdownTextarea()?.value)
    expect(blobArg.type).toContain("markdown")
    return aiMarkdownTextarea()?.value
  }

  it("opens with unchecked inclusion and usual focus context", async () => {
    const note = mountForm()
    await flushPromises()

    expect((inclusionCheckbox().element as HTMLInputElement).checked).toBe(
      false
    )
    expect(NoteExportController.getAiContextMarkdown).toHaveBeenCalledWith({
      path: { note: note.id },
      query: { tokenLimit: 2000 },
    })
    expect(aiMarkdownTextarea()?.value).toBe(ordinaryMarkdown)
    expect(wrapper.text()).toContain(
      "Selected questions form a complete appendix outside the note-body token budget."
    )
  })

  it("checks inclusion then refreshes so preview, clipboard, and download match", async () => {
    const note = mountForm()
    await flushPromises()
    vi.mocked(NoteExportController.getAiContextMarkdown).mockResolvedValueOnce(
      wrapSdkResponse({ markdown: withQuestionsMarkdown })
    )

    await setInclusion(true)

    expect(NoteExportController.getAiContextMarkdown).toHaveBeenLastCalledWith({
      path: { note: note.id },
      query: { tokenLimit: 2000, includeRecentQuestions: true },
    })
    expect(aiMarkdownTextarea()?.value).toBe(withQuestionsMarkdown)
    expect(await copyAndDownloadText()).toBe(withQuestionsMarkdown)
    expect(vi.mocked(saveAs).mock.calls.at(-1)![1]).toBe(
      `note-${note.id}-focus-context.md`
    )
  })

  it("clears inclusion and returns ordinary context", async () => {
    const note = mountForm()
    await flushPromises()
    vi.mocked(NoteExportController.getAiContextMarkdown)
      .mockResolvedValueOnce(
        wrapSdkResponse({ markdown: withQuestionsMarkdown })
      )
      .mockResolvedValueOnce(wrapSdkResponse({ markdown: ordinaryMarkdown }))

    await setInclusion(true)
    expect(aiMarkdownTextarea()?.value).toBe(withQuestionsMarkdown)

    await setInclusion(false)

    expect(NoteExportController.getAiContextMarkdown).toHaveBeenLastCalledWith({
      path: { note: note.id },
      query: { tokenLimit: 2000 },
    })
    expect(aiMarkdownTextarea()?.value).toBe(ordinaryMarkdown)
  })

  it("clears stale text and disables copy/download while inclusion refresh is pending", async () => {
    mountForm()
    await flushPromises()
    expect(aiMarkdownTextarea()?.value).toBe(ordinaryMarkdown)

    let release!: () => void
    const pending = new Promise<void>((resolve) => {
      release = resolve
    })
    vi.mocked(NoteExportController.getAiContextMarkdown).mockImplementationOnce(
      async () => {
        await pending
        return wrapSdkResponse({ markdown: withQuestionsMarkdown })
      }
    )

    await setInclusion(true)
    expect(aiMarkdownTextarea()?.value).toBe("")
    expectExportActions(false)

    release()
    await flushPromises()

    expect(aiMarkdownTextarea()?.value).toBe(withQuestionsMarkdown)
    expectExportActions(true)
  })

  it("shows shared API feedback after failure, blocks stale export, and recovers via Refresh", async () => {
    mountForm()
    await flushPromises()
    expect(aiMarkdownTextarea()?.value).toBe(ordinaryMarkdown)

    vi.mocked(NoteExportController.getAiContextMarkdown).mockResolvedValueOnce(
      wrapSdkError({ message: "Export unavailable" })
    )
    await setInclusion(true)

    expect(aiMarkdownTextarea()?.value).toBe("")
    expectExportActions(false)
    expect(toastMessage(await toastShown("error"))).toContain(
      "Export unavailable"
    )

    vi.mocked(NoteExportController.getAiContextMarkdown).mockResolvedValueOnce(
      wrapSdkResponse({ markdown: withQuestionsMarkdown })
    )
    await clickTestId("refresh-context-md-btn")

    expect(aiMarkdownTextarea()?.value).toBe(withQuestionsMarkdown)
    expectExportActions(true)
  })

  it("refresh markdown refetches with current token budget", async () => {
    const note = makeMe.aNote.please()
    const md1 = { markdown: "first" }
    const md2 = { markdown: "second-budget" }
    vi.mocked(NoteExportController.getAiContextMarkdown).mockReset()
    vi.mocked(NoteExportController.getAiContextMarkdown)
      .mockResolvedValueOnce(wrapSdkResponse(md1))
      .mockResolvedValueOnce(wrapSdkResponse(md2))
    mountForm(note)
    await flushPromises()

    await setTokenLimit("3000")
    await clickTestId("refresh-context-md-btn")

    expect(aiMarkdownTextarea()?.value).toContain("second-budget")
    expect(NoteExportController.getAiContextMarkdown).toHaveBeenLastCalledWith({
      path: { note: note.id },
      query: { tokenLimit: 3000 },
    })
  })
})
