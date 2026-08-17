import { flushPromises } from "@vue/test-utils"
import { wrapSdkResponse } from "@tests/helpers"
import { settleScheduledAutofocus } from "@tests/helpers/focusTargetTestSupport"
import {
  mountNoteNewForm,
  noteNewFormNote,
  noteNewFormRealm,
  notebookRootProps,
  setNoteNewFormTitle,
  setupNoteNewFormSdkMocks,
  type NoteNewFormSdkSpies,
} from "@tests/notes/noteNewFormTestSupport"
import { describe, it, expect, beforeEach, afterEach, vi } from "vitest"

const popupsMock = {
  confirm: vi.fn().mockResolvedValue(false),
  alert: vi.fn(),
  options: vi.fn(),
  done: vi.fn(),
  register: vi.fn(),
  peek: vi.fn(),
}

vi.mock("@/components/commons/Popups/usePopups", () => ({
  default: () => ({ popups: popupsMock }),
}))

vi.mock("vue-router", async (importOriginal) => {
  const actual = await importOriginal<typeof import("vue-router")>()
  return {
    ...actual,
    useRouter: () => ({
      currentRoute: { value: {} },
    }),
    useRoute: () => ({ path: "/", fullPath: "/" }),
  }
})

describe("adding new note", () => {
  let sdkSpies: NoteNewFormSdkSpies

  beforeEach(() => {
    vi.useFakeTimers()
    vi.resetAllMocks()
    popupsMock.confirm.mockReset()
    popupsMock.confirm.mockResolvedValue(false)
    sdkSpies = setupNoteNewFormSdkMocks()
  })

  afterEach(() => {
    if (vi.isFakeTimers()) {
      vi.runOnlyPendingTimers()
    }
    vi.useRealTimers()
  })

  it("does not search for initial default 'Untitled' title", async () => {
    sdkSpies.searchForRelationshipTargetWithinSpy.mockResolvedValue(
      wrapSdkResponse([])
    )
    const wrapper = mountNoteNewForm(notebookRootProps, {
      attachTo: document.body,
    })

    vi.runOnlyPendingTimers()
    await flushPromises()

    expect(sdkSpies.searchForRelationshipTargetWithinSpy).not.toHaveBeenCalled()
    wrapper.unmount()
  })

  it("submits initialTitle as newTitle when unchanged", async () => {
    const wrapper = mountNoteNewForm({
      ...notebookRootProps,
      initialTitle: "2026-05-09",
    })

    await wrapper.find('[data-testid="note-new-form"]').trigger("submit")
    await flushPromises()
    expect(sdkSpies.mockedCreateNoteAtRoot).toHaveBeenCalledWith({
      path: { notebook: noteNewFormRealm.notebookRealm.notebook.id },
      body: expect.objectContaining({ newTitle: "2026-05-09 " }),
    })
    wrapper.unmount()
  })

  it("searches when user edits title back to 'Untitled'", async () => {
    sdkSpies.searchForRelationshipTargetWithinSpy.mockResolvedValue(
      wrapSdkResponse([
        {
          hitKind: "NOTE",
          noteSearchResult: {
            noteTopology: noteNewFormNote.noteTopology,
            notebookId: 1,
            distance: 0.9,
          },
        },
      ])
    )
    const wrapper = mountNoteNewForm(notebookRootProps, {
      attachTo: document.body,
    })

    await setNoteNewFormTitle(wrapper, "myth")
    vi.runOnlyPendingTimers()
    await flushPromises()
    sdkSpies.searchForRelationshipTargetWithinSpy.mockClear()
    await setNoteNewFormTitle(wrapper, "Untitled")
    vi.runOnlyPendingTimers()
    await flushPromises()

    expect(sdkSpies.searchForRelationshipTargetWithinSpy).toHaveBeenCalledWith({
      path: { note: noteNewFormNote.id },
      body: expect.objectContaining({ searchKey: "Untitled" }),
    })
    wrapper.unmount()
  })

  it("search for duplicate", async () => {
    sdkSpies.searchForRelationshipTargetWithinSpy.mockResolvedValue(
      wrapSdkResponse([
        {
          hitKind: "NOTE",
          noteSearchResult: {
            noteTopology: noteNewFormNote.noteTopology,
            notebookId: 1,
            distance: 0.9,
          },
        },
      ])
    )
    const wrapper = mountNoteNewForm(notebookRootProps, {
      attachTo: document.body,
    })
    await setNoteNewFormTitle(wrapper, "myth")
    vi.runOnlyPendingTimers()
    await flushPromises()

    expect(wrapper.text()).toContain("mythical")
    expect(sdkSpies.searchForRelationshipTargetWithinSpy).toHaveBeenCalledWith({
      path: { note: noteNewFormNote.id },
      body: expect.objectContaining({ searchKey: "myth" }),
    })
    expect(sdkSpies.semanticSearchWithinSpy).not.toHaveBeenCalled()
    wrapper.unmount()
  })

  it("runs semantic search when the semantic toggle is turned on", async () => {
    sdkSpies.searchForRelationshipTargetWithinSpy.mockResolvedValue(
      wrapSdkResponse([])
    )
    sdkSpies.semanticSearchWithinSpy.mockResolvedValue(wrapSdkResponse([]))
    const wrapper = mountNoteNewForm()
    await setNoteNewFormTitle(wrapper, "myth")
    vi.runOnlyPendingTimers()

    sdkSpies.semanticSearchWithinSpy.mockClear()
    sdkSpies.searchForRelationshipTargetWithinSpy.mockClear()

    await wrapper
      .find('[data-testid="note-new-form-semantic-search-toggle"]')
      .trigger("click")
    vi.runOnlyPendingTimers()

    expect(sdkSpies.semanticSearchWithinSpy).toHaveBeenCalledWith({
      path: { note: noteNewFormNote.id },
      body: expect.objectContaining({ searchKey: "myth" }),
    })
    wrapper.unmount()
  })

  it("selects all text when the default Untitled title is shown", async () => {
    vi.useRealTimers()
    const wrapper = mountNoteNewForm(notebookRootProps, {
      attachTo: document.body,
    })

    await settleScheduledAutofocus()

    expect(document.activeElement?.classList.contains("seamless-editor")).toBe(
      true
    )
    expect(window.getSelection()?.toString()).toBe("Untitled")
    wrapper.unmount()
  })

  it("places the caret after a trailing space when initialTitle comes from a template", async () => {
    vi.useRealTimers()
    const wrapper = mountNoteNewForm(
      {
        ...notebookRootProps,
        initialTitle: "2026-05-09",
      },
      { attachTo: document.body }
    )

    await settleScheduledAutofocus()

    const editor = document.activeElement as HTMLElement
    expect(editor.classList.contains("seamless-editor")).toBe(true)
    expect(editor.textContent).toBe("2026-05-09 ")
    const range = window.getSelection()?.getRangeAt(0)
    expect(range?.collapsed).toBe(true)
    const afterCaret = range!.cloneRange()
    afterCaret.selectNodeContents(editor)
    afterCaret.setStart(range!.endContainer, range!.endOffset)
    expect(afterCaret.toString()).toBe("")
    wrapper.unmount()
  })

  it("does not add a second space when the template already ends with a space", async () => {
    const wrapper = mountNoteNewForm({
      ...notebookRootProps,
      initialTitle: "2026-05-09 ",
    })

    expect(
      (wrapper.find('[data-test="note-title"]').element as HTMLElement)
        .textContent
    ).toBe("2026-05-09 ")
    wrapper.unmount()
  })
})
