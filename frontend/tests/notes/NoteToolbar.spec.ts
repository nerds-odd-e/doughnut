import {
  NoteController,
  SearchController,
} from "@generated/doughnut-backend-api/sdk.gen"
import type { NoteRealm } from "@generated/doughnut-backend-api"
import NoteToolbar from "@/components/notes/core/NoteToolbar.vue"
import NoteMoreOptionsForm from "@/components/notes/widgets/NoteMoreOptionsForm.vue"
import makeMe from "doughnut-test-fixtures/makeMe"
import helper from "@tests/helpers"
import { mockSdkService } from "@tests/helpers"
import { notebookSidebarClosedPlugin } from "@tests/helpers/notebookSidebarTestProvide"
import { screen } from "@testing-library/vue"
import { describe, it, expect, afterEach, vi } from "vitest"
import { type VueWrapper, flushPromises } from "@vue/test-utils"
import { createRouter, createWebHistory } from "vue-router"
import routes from "@/routes/routes"

function noteToolbarProps(
  noteRealm: NoteRealm,
  overrides: Record<string, unknown> = {}
) {
  return {
    note: noteRealm.note,
    notebookId: noteRealm.notebookRealm.notebook.id,
    activeNoteRealm: noteRealm,
    ...overrides,
  }
}

describe("NoteToolbar", () => {
  // biome-ignore lint/suspicious/noExplicitAny: wrapper for testing
  let wrapper: VueWrapper<any>

  afterEach(() => {
    wrapper?.unmount()
    document.body.innerHTML = ""
  })

  it("routes to note show by id when starting a conversation about the note", async () => {
    const router = createRouter({
      history: createWebHistory(),
      routes,
    })
    const pushSpy = vi.spyOn(router, "push")
    const noteRealm = makeMe.aNoteRealm.title("Dummy Title").please()
    mockSdkService(
      NoteController,
      "getNoteInfo",
      makeMe.aNoteRecallInfo
        .recallSetting({
          level: 0,
          rememberSpelling: false,
          skipMemoryTracking: false,
        })
        .please()
    )

    wrapper = helper
      .component(NoteToolbar)
      .withRouter(router)
      .withCleanStorage()
      .withProps(noteToolbarProps(noteRealm))
      .mount({ attachTo: document.body })

    await flushPromises()

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

  it("displays menu items when dropdown is open", async () => {
    const noteRealm = makeMe.aNoteRealm.title("Dummy Title").please()
    mockSdkService(
      NoteController,
      "getNoteInfo",
      makeMe.aNoteRecallInfo
        .recallSetting({
          level: 0,
          rememberSpelling: false,
          skipMemoryTracking: false,
        })
        .please()
    )

    wrapper = helper
      .component(NoteToolbar)
      .withRouter()
      .withCleanStorage()
      .withProps(noteToolbarProps(noteRealm))
      .mount({ attachTo: document.body })

    await flushPromises()

    const moreOptionsButton = wrapper.find('[title="more options"]')

    // Simulate a click event on the button to open the dialog
    await moreOptionsButton.trigger("click")
    await flushPromises()

    // Check if the dialog component exists
    const dialog = wrapper.findComponent(NoteMoreOptionsForm)
    expect(dialog.exists()).toBe(true)

    // Menu panel is portaled to body
    expect(
      document.querySelector("[data-dropdown-portal-panel]")
    ).not.toBeNull()
    expect(
      document.querySelector('button[title="Questions for the note"]')
    ).not.toBeNull()
  })

  it("closes more options dialog when note id changes", async () => {
    const noteRealm = makeMe.aNoteRealm.title("Dummy Title").please()
    mockSdkService(
      NoteController,
      "getNoteInfo",
      makeMe.aNoteRecallInfo
        .recallSetting({
          level: 0,
          rememberSpelling: false,
          skipMemoryTracking: false,
        })
        .please()
    )

    wrapper = helper
      .component(NoteToolbar)
      .withRouter()
      .withCleanStorage()
      .withProps(noteToolbarProps(noteRealm))
      .mount({ attachTo: document.body })

    await flushPromises()

    const moreOptionsButton = wrapper.find('[title="more options"]')

    // Open the dialog
    await moreOptionsButton.trigger("click")
    await flushPromises()

    // Verify dialog is open
    const dialog = wrapper.findComponent(NoteMoreOptionsForm)
    expect(dialog.exists()).toBe(true)

    // Change the note id
    const newNote = makeMe.aNoteRealm.title("New Note").please()
    await wrapper.setProps(noteToolbarProps(newNote))
    await flushPromises()

    // Verify dialog is closed (still mounted; details hides dropdown content)
    const details = wrapper.find("[data-auto-collapse-dropdown]")
    expect((details.element as HTMLDetailsElement).open).toBe(false)
  })

  it("opens Link search on Ctrl+Shift+F when not readonly", async () => {
    const noteRealm = makeMe.aNoteRealm.title("Dummy Title").please()
    mockSdkService(
      NoteController,
      "getNoteInfo",
      makeMe.aNoteRecallInfo
        .recallSetting({
          level: 0,
          rememberSpelling: false,
          skipMemoryTracking: false,
        })
        .please()
    )
    mockSdkService(SearchController, "searchForRelationshipTarget", [])
    mockSdkService(SearchController, "searchForRelationshipTargetWithin", [])
    mockSdkService(SearchController, "semanticSearch", [])
    mockSdkService(SearchController, "semanticSearchWithin", [])

    wrapper = helper
      .component(NoteToolbar)
      .withRouter()
      .withCleanStorage()
      .withProps(noteToolbarProps(noteRealm))
      .mount({ attachTo: document.body })

    await flushPromises()
    expect(screen.queryByPlaceholderText("Search")).toBeNull()

    window.dispatchEvent(
      new KeyboardEvent("keydown", {
        key: "f",
        code: "KeyF",
        ctrlKey: true,
        shiftKey: true,
        bubbles: true,
        cancelable: true,
      })
    )
    await flushPromises()

    expect(await screen.findByPlaceholderText("Search")).toBeInTheDocument()
  })

  it("does not open Link search on Ctrl+Shift+F when readonly", async () => {
    const noteRealm = makeMe.aNoteRealm.title("Dummy Title").please()
    mockSdkService(
      NoteController,
      "getNoteInfo",
      makeMe.aNoteRecallInfo
        .recallSetting({
          level: 0,
          rememberSpelling: false,
          skipMemoryTracking: false,
        })
        .please()
    )

    wrapper = helper
      .component(NoteToolbar)
      .withRouter()
      .withCleanStorage()
      .withProps(noteToolbarProps(noteRealm, { readonly: true }))
      .mount({ attachTo: document.body })

    await flushPromises()
    window.dispatchEvent(
      new KeyboardEvent("keydown", {
        key: "f",
        code: "KeyF",
        ctrlKey: true,
        shiftKey: true,
        bubbles: true,
        cancelable: true,
      })
    )
    await flushPromises()

    expect(screen.queryByPlaceholderText("Search")).toBeNull()
  })

  it("shows New note when sidebar is collapsed", async () => {
    const noteRealm = makeMe.aNoteRealm.title("Dummy Title").please()
    mockSdkService(
      NoteController,
      "getNoteInfo",
      makeMe.aNoteRecallInfo
        .recallSetting({
          level: 0,
          rememberSpelling: false,
          skipMemoryTracking: false,
        })
        .please()
    )

    wrapper = helper
      .component(NoteToolbar)
      .withRouter()
      .withCleanStorage()
      .withProps(noteToolbarProps(noteRealm))
      .withPlugin(notebookSidebarClosedPlugin())
      .mount({ attachTo: document.body })

    await flushPromises()
    expect(wrapper.find('button[title="New note"]').exists()).toBe(true)
  })
})
