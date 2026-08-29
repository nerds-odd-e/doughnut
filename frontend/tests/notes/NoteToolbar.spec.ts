import { SearchController } from "@generated/donut-backend-api/sdk.gen"
import makeMe from "donut-test-fixtures/makeMe"
import helper, { mockSdkService } from "@tests/helpers"
import { notebookSidebarClosedPlugin } from "@tests/helpers/notebookSidebarTestProvide"
import {
  installMockResizeObserver,
  restoreNoteToolbarWidthMocks,
} from "@tests/helpers/mockNoteToolbarNavWidth"
import {
  mountNoteToolbar,
  resetNoteToolbarTestState,
} from "@tests/notes/noteToolbarTestHelpers"
import { mountNoteToolbarAt } from "@tests/notes/noteToolbarRouteMount"
import { wrapWithNoteShortcutScope } from "@tests/helpers/noteShortcutScopeTestHelpers"
import NoteToolbar from "@/components/notes/core/NoteToolbar.vue"
import { noteToolbarEditTitles } from "@/components/notes/widgets/noteMoreOptionsTitles"
import {
  notePropertyLocation,
  noteShowLocation,
} from "@/routes/noteShowLocation"
import { screen } from "@testing-library/vue"
import { describe, it, expect, afterEach, beforeEach, vi } from "vitest"
import { type VueWrapper, flushPromises } from "@vue/test-utils"

describe("NoteToolbar", () => {
  // biome-ignore lint/suspicious/noExplicitAny: wrapper for testing
  let wrapper: VueWrapper<any>

  afterEach(() => {
    wrapper?.unmount()
    document.body.innerHTML = ""
    restoreNoteToolbarWidthMocks()
    vi.unstubAllGlobals()
  })

  beforeEach(() => {
    installMockResizeObserver()
    resetNoteToolbarTestState()
  })

  it("replaces conversation query on the current note location", async () => {
    const mounted = await mountNoteToolbarAt(noteShowLocation)
    wrapper = mounted.wrapper
    const { router, noteRealm } = mounted
    const replaceSpy = vi.spyOn(router, "replace")
    const pushSpy = vi.spyOn(router, "push")

    await wrapper
      .find('[title="Start a conversation about this note"]')
      .trigger("click")
    await flushPromises()

    expect(pushSpy).not.toHaveBeenCalled()
    expect(replaceSpy).toHaveBeenCalledTimes(1)
    expect(router.currentRoute.value).toMatchObject({
      ...noteShowLocation(noteRealm.note.id),
      query: { conversation: "true" },
    })
  })

  it("keeps the focused property when starting a conversation", async () => {
    const mounted = await mountNoteToolbarAt((noteId) =>
      notePropertyLocation(noteId, "topic")
    )
    wrapper = mounted.wrapper
    const { router, noteRealm } = mounted

    await wrapper
      .find('[title="Start a conversation about this note"]')
      .trigger("click")
    await flushPromises()

    expect(router.currentRoute.value).toMatchObject(
      notePropertyLocation(noteRealm.note.id, "topic")
    )
    expect(router.currentRoute.value.query).toEqual({ conversation: "true" })
  })

  function dispatchWikiLinkOrRelationshipShortcut() {
    document.dispatchEvent(
      new KeyboardEvent("keydown", {
        key: "f",
        code: "KeyF",
        ctrlKey: true,
        shiftKey: true,
        bubbles: true,
        cancelable: true,
      })
    )
  }

  it("names the connect control as wiki link or relationship", async () => {
    const noteRealm = makeMe.aNoteRealm.title("Dummy Title").please()

    wrapper = await mountNoteToolbar(noteRealm)

    expect(
      wrapper.find('[aria-label="Wiki link or relationship"]').exists()
    ).toBe(true)
    expect(
      wrapper
        .find(
          '[title="Wiki link or relationship (Ctrl+Shift+F / Cmd+Shift+F)"]'
        )
        .exists()
    ).toBe(true)
  })

  it("opens wiki link or relationship search on Ctrl+Shift+F when not readonly", async () => {
    const noteRealm = makeMe.aNoteRealm.title("Dummy Title").please()
    mockSdkService(SearchController, "searchForRelationshipTarget", [])
    mockSdkService(SearchController, "searchForRelationshipTargetWithin", [])
    mockSdkService(SearchController, "semanticSearch", [])
    mockSdkService(SearchController, "semanticSearchWithin", [])

    wrapper = await mountNoteToolbar(noteRealm)
    expect(screen.queryByPlaceholderText("Search")).toBeNull()

    dispatchWikiLinkOrRelationshipShortcut()
    await flushPromises()

    expect(await screen.findByPlaceholderText("Search")).toBeInTheDocument()
  })

  it("does not open wiki link or relationship search on Ctrl+Shift+F when readonly", async () => {
    const noteRealm = makeMe.aNoteRealm.title("Dummy Title").please()

    wrapper = await mountNoteToolbar(noteRealm, {
      propsOverrides: { readonly: true },
    })
    dispatchWikiLinkOrRelationshipShortcut()
    await flushPromises()

    expect(screen.queryByPlaceholderText("Search")).toBeNull()
  })

  it("shows New note when sidebar is collapsed", async () => {
    const noteRealm = makeMe.aNoteRealm.title("Dummy Title").please()

    wrapper = await mountNoteToolbar(noteRealm, {
      plugin: notebookSidebarClosedPlugin(),
    })
    expect(wrapper.find('button[title="New note (n)"]').exists()).toBe(true)
  })

  function dispatchToggleEditModeShortcut() {
    document.dispatchEvent(
      new KeyboardEvent("keydown", {
        key: "m",
        code: "KeyM",
        bubbles: true,
        cancelable: true,
      })
    )
  }

  it.each([
    { asMarkdown: false, expected: true },
    { asMarkdown: true, expected: false },
  ])(
    "emits edit-as-markdown=$expected when m is pressed (asMarkdown=$asMarkdown)",
    async ({ asMarkdown, expected }) => {
      const noteRealm = makeMe.aNoteRealm.title("Dummy Title").please()

      wrapper = await mountNoteToolbar(noteRealm, {
        propsOverrides: { asMarkdown },
      })

      dispatchToggleEditModeShortcut()
      await flushPromises()

      expect(wrapper.emitted("edit-as-markdown")).toEqual([[expected]])
    }
  )

  it("advertises keyboard shortcut hints on edit mode buttons", async () => {
    const noteRealm = makeMe.aNoteRealm.title("Dummy Title").please()

    wrapper = await mountNoteToolbar(noteRealm, {
      propsOverrides: { asMarkdown: false },
    })
    expect(
      wrapper.find(`button[title="${noteToolbarEditTitles.markdown}"]`).exists()
    ).toBe(true)

    await wrapper.setProps({ asMarkdown: true })
    expect(
      wrapper.find(`button[title="${noteToolbarEditTitles.rich}"]`).exists()
    ).toBe(true)
  })

  it("does not emit edit-as-markdown when m is pressed and readonly", async () => {
    const noteRealm = makeMe.aNoteRealm.title("Dummy Title").please()

    wrapper = await mountNoteToolbar(noteRealm, {
      propsOverrides: { readonly: true },
    })

    dispatchToggleEditModeShortcut()
    await flushPromises()

    expect(wrapper.emitted("edit-as-markdown")).toBeUndefined()
  })

  it("does not emit edit-as-markdown when m is pressed and readonly", async () => {
    const noteRealm = makeMe.aNoteRealm.title("Dummy Title").please()

    const Harness = wrapWithNoteShortcutScope(
      NoteToolbar,
      {
        note: noteRealm.note,
        notebookId: noteRealm.notebookRealm.notebook.id,
        activeNoteRealm: noteRealm,
      },
      false
    )
    wrapper = helper
      .component(Harness)
      .withCleanStorage()
      .withRouter()
      .mount({ attachTo: document.body })
    await flushPromises()

    dispatchToggleEditModeShortcut()
    await flushPromises()

    expect(wrapper.emitted("edit-as-markdown")).toBeUndefined()
  })
})
