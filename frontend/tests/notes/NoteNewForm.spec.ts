import { WikidataController } from "@generated/donut-backend-api/sdk.gen"
import { type VueWrapper, flushPromises } from "@vue/test-utils"
import type { ComponentPublicInstance } from "vue"
import { mockSdkService, wrapSdkResponse } from "@tests/helpers"
import { settleScheduledAutofocus } from "@tests/helpers/focusTargetTestSupport"
import {
  mountNoteNewForm,
  noteNewFormNote,
  noteTitleText,
  notebookRootProps,
  openWikidataDialog,
  resolveWikidataSearch,
  selectWikidataSearchResult,
  setNoteNewFormTitle,
  setupNoteNewFormSdkMocks,
  wikidataCancelButton,
  wikidataDialogIsOpen,
  type NoteNewFormSdkSpies,
} from "@tests/notes/noteNewFormTestSupport"
import { describe, it, expect, beforeEach, afterEach, vi } from "vitest"

describe("adding new note", () => {
  let sdkSpies: NoteNewFormSdkSpies
  let wrapper: VueWrapper<ComponentPublicInstance>

  beforeEach(() => {
    vi.useFakeTimers()
    vi.resetAllMocks()
    sdkSpies = setupNoteNewFormSdkMocks()
  })

  afterEach(() => {
    wrapper?.unmount()
    vi.runOnlyPendingTimers()
    vi.useRealTimers()
  })

  it("does not search for initial default 'Untitled' title", async () => {
    wrapper = mountNoteNewForm(notebookRootProps, { attachTo: document.body })

    vi.runOnlyPendingTimers()
    await flushPromises()

    expect(sdkSpies.searchForRelationshipTargetWithinSpy).not.toHaveBeenCalled()
  })

  it.each(["light", "dark"])(
    "keeps title search within the %s form and Submit clickable",
    async (theme) => {
      wrapper = mountNoteNewForm(
        { ...notebookRootProps, initialTitle: "2026-10-10" },
        { attachTo: document.body }
      )
      const form = wrapper.element as HTMLElement
      form.setAttribute("data-theme", theme)
      form.classList.add("bg-base-100")
      form.style.width = "640px"
      vi.runOnlyPendingTimers()
      await flushPromises()

      expect(wrapper.text()).toContain("No matching notes found.")
      const results = form.querySelector(".title-search-results") as HTMLElement
      const submit = form.querySelector(
        'input[type="submit"]'
      ) as HTMLInputElement
      const panelBounds = results.getBoundingClientRect()
      const submitBounds = submit.getBoundingClientRect()
      expect(panelBounds.bottom).toBeLessThanOrEqual(submitBounds.top)
      expect(getComputedStyle(results).backgroundColor).toBe(
        getComputedStyle(form).backgroundColor
      )
      expect(
        document.elementFromPoint(
          submitBounds.left + submitBounds.width / 2,
          submitBounds.top + submitBounds.height / 2
        )
      ).toBe(submit)
      submit.click()
      await flushPromises()
      expect(sdkSpies.mockedCreateNoteAtRoot).toHaveBeenCalled()
    }
  )

  it("searches for duplicate titles, including after returning to the default title", async () => {
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
    wrapper = mountNoteNewForm(notebookRootProps, { attachTo: document.body })

    await setNoteNewFormTitle(wrapper, "myth")
    vi.runOnlyPendingTimers()
    await flushPromises()

    expect(wrapper.text()).toContain("mythical")
    expect(sdkSpies.searchForRelationshipTargetWithinSpy).toHaveBeenCalledWith({
      path: { note: noteNewFormNote.id },
      body: expect.objectContaining({ searchKey: "myth" }),
    })

    sdkSpies.searchForRelationshipTargetWithinSpy.mockClear()
    await setNoteNewFormTitle(wrapper, "Untitled")
    vi.runOnlyPendingTimers()
    await flushPromises()

    expect(sdkSpies.searchForRelationshipTargetWithinSpy).toHaveBeenCalledWith({
      path: { note: noteNewFormNote.id },
      body: expect.objectContaining({ searchKey: "Untitled" }),
    })
  })

  it("selects all text when the default Untitled title is shown", async () => {
    wrapper = mountNoteNewForm(notebookRootProps, { attachTo: document.body })

    await settleScheduledAutofocus()

    expect(document.activeElement?.classList.contains("seamless-editor")).toBe(
      true
    )
    expect(window.getSelection()?.toString()).toBe("Untitled")
  })

  it("places the caret after a trailing space when initialTitle comes from a template", async () => {
    wrapper = mountNoteNewForm(
      { ...notebookRootProps, initialTitle: "2026-05-09" },
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
  })

  it("does not add a second space when the template already ends with a space", async () => {
    wrapper = mountNoteNewForm({
      ...notebookRootProps,
      initialTitle: "2026-05-09 ",
    })

    expect(noteTitleText(wrapper)).toBe("2026-05-09 ")
  })

  describe("search wikidata entry", () => {
    let searchWikidataSpy: ReturnType<typeof mockSdkService>

    beforeEach(() => {
      searchWikidataSpy = mockSdkService(
        WikidataController,
        "searchWikidata",
        []
      )
      wrapper = mountNoteNewForm(notebookRootProps, { attachTo: document.body })
    })

    it("closes on cancel then applies a matching-title selection", async () => {
      resolveWikidataSearch(searchWikidataSpy, "Dog", "Q1")
      await openWikidataDialog(wrapper, "dog")
      expect(searchWikidataSpy).toHaveBeenCalledWith({
        query: { search: "dog" },
      })
      expect(wikidataDialogIsOpen()).toBe(true)

      wikidataCancelButton().click()
      await flushPromises()
      expect(wikidataDialogIsOpen()).toBe(false)

      await openWikidataDialog(wrapper, "dog")
      await selectWikidataSearchResult("Q1")
      expect(wikidataDialogIsOpen()).toBe(false)
      expect(noteTitleText(wrapper)).toBe("Dog")
    })

    it("applies replace title action for a differing wikidata label", async () => {
      resolveWikidataSearch(searchWikidataSpy, "Canine", "Q1")
      await openWikidataDialog(wrapper, "dog")
      await selectWikidataSearchResult("Q1", "Replace")
      expect(noteTitleText(wrapper)).toBe("Canine")
    })
  })
})
