import { SearchController } from "@generated/doughnut-backend-api/sdk.gen"
import makeMe from "doughnut-test-fixtures/makeMe"
import helper, { mockSdkService } from "@tests/helpers"
import { notebookSidebarClosedPlugin } from "@tests/helpers/notebookSidebarTestProvide"
import { installMockResizeObserver } from "@tests/helpers/mockNoteToolbarNavWidth"
import { mountNoteToolbar } from "@tests/notes/noteToolbarTestHelpers"
import { wrapWithNoteShortcutScope } from "@tests/helpers/noteShortcutScopeTestHelpers"
import NoteToolbar from "@/components/notes/core/NoteToolbar.vue"
import { screen } from "@testing-library/vue"
import { describe, it, expect, afterEach, beforeEach, vi } from "vitest"
import { type VueWrapper, flushPromises } from "@vue/test-utils"
import { createRouter, createWebHistory } from "vue-router"
import routes from "@/routes/routes"

describe("NoteToolbar", () => {
  // biome-ignore lint/suspicious/noExplicitAny: wrapper for testing
  let wrapper: VueWrapper<any>

  afterEach(() => {
    wrapper?.unmount()
    document.body.innerHTML = ""
    vi.unstubAllGlobals()
  })

  beforeEach(() => {
    installMockResizeObserver()
  })

  it("routes to note show by id when starting a conversation about the note", async () => {
    const router = createRouter({
      history: createWebHistory(),
      routes,
    })
    const pushSpy = vi.spyOn(router, "push")
    const noteRealm = makeMe.aNoteRealm.title("Dummy Title").please()

    wrapper = await mountNoteToolbar(noteRealm, { router })
    await wrapper
      .find('[title="Star a conversation about this note"]')
      .trigger("click")
    await flushPromises()

    expect(pushSpy).toHaveBeenCalledWith({
      name: "noteShow",
      params: {
        noteId: String(noteRealm.note.id),
      },
      query: { conversation: "true" },
    })
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
    expect(wrapper.find('button[title="Edit as markdown (m)"]').exists()).toBe(
      true
    )

    await wrapper.setProps({ asMarkdown: true })
    expect(
      wrapper.find('button[title="Edit as rich content (m)"]').exists()
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

  it("toggles the audio tools panel from the toolbar button", async () => {
    const noteRealm = makeMe.aNoteRealm.title("Dummy Title").please()

    wrapper = await mountNoteToolbar(noteRealm)
    const audioToolsButton = wrapper.find('button[title="Audio tools"]')

    expect(audioToolsButton.exists()).toBe(true)
    expect(audioToolsButton.classes()).not.toContain("daisy-btn-active")
    expect(
      wrapper.find('[data-testid="note-toolbar-panel-shell"]').exists()
    ).toBe(false)

    await audioToolsButton.trigger("click")
    await flushPromises()

    expect(
      wrapper.find('[data-testid="note-toolbar-panel-shell"]').exists()
    ).toBe(true)
    expect(audioToolsButton.classes()).toContain("daisy-btn-active")
    expect(audioToolsButton.attributes("aria-pressed")).toBe("true")

    await audioToolsButton.trigger("click")
    await flushPromises()

    expect(
      wrapper.find('[data-testid="note-toolbar-panel-shell"]').exists()
    ).toBe(false)
    expect(audioToolsButton.classes()).not.toContain("daisy-btn-active")
    expect(audioToolsButton.attributes("aria-pressed")).toBe("false")
  })

  it("does not emit edit-as-markdown when m is pressed and shortcut scope is inactive", async () => {
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
