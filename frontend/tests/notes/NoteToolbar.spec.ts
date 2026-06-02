import { SearchController } from "@generated/doughnut-backend-api/sdk.gen"
import makeMe from "doughnut-test-fixtures/makeMe"
import { mockSdkService } from "@tests/helpers"
import { notebookSidebarClosedPlugin } from "@tests/helpers/notebookSidebarTestProvide"
import { installMockResizeObserver } from "@tests/helpers/mockNoteToolbarNavWidth"
import { mountNoteToolbar } from "@tests/notes/noteToolbarTestHelpers"
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

  it("opens Link search on Ctrl+Shift+F when not readonly", async () => {
    const noteRealm = makeMe.aNoteRealm.title("Dummy Title").please()
    mockSdkService(SearchController, "searchForRelationshipTarget", [])
    mockSdkService(SearchController, "searchForRelationshipTargetWithin", [])
    mockSdkService(SearchController, "semanticSearch", [])
    mockSdkService(SearchController, "semanticSearchWithin", [])

    wrapper = await mountNoteToolbar(noteRealm)
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

    wrapper = await mountNoteToolbar(noteRealm, {
      propsOverrides: { readonly: true },
    })
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

    wrapper = await mountNoteToolbar(noteRealm, {
      plugin: notebookSidebarClosedPlugin(),
    })
    expect(wrapper.find('button[title^="New note"]').exists()).toBe(true)
  })
})
