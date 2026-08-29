import { NotebookController } from "@generated/donut-backend-api/sdk.gen"
import NotebookSidebarLayout from "@/layouts/NotebookSidebarLayout.vue"
import { noteShowLocation } from "@/routes/noteShowLocation"
import routes from "@/routes/routes"
import { useStorageAccessor } from "@/composables/useStorageAccessor"
import makeMe from "donut-test-fixtures/makeMe"
import helper, { mockSdkServiceWithImplementation } from "@tests/helpers"
import { flushPromises } from "@vue/test-utils"
import { afterEach, beforeEach, describe, expect, it, vi } from "vitest"
import { createRouter, createWebHistory } from "vue-router"
import { sidebarDefaultTreeFixtures } from "./sidebarDefaultTree"
import {
  neverResolving,
  prepareSidebarDefaultMountContext,
  sidebarShowsActiveItem,
  stubShowNotePending,
  teardownSidebarComponentTest,
  uncachedNoteInSameNotebook,
} from "./sidebarTestSupport"

describe("Sidebar route navigation: sticky realm during uncached note load", () => {
  // biome-ignore lint/suspicious/noExplicitAny: wrapper for testing
  let wrapper: import("@vue/test-utils").VueWrapper<any>
  const storageAccessor = useStorageAccessor()
  const fixtures = sidebarDefaultTreeFixtures
  const notebookName = fixtures.topNoteRealm.notebookRealm.notebook.name

  beforeEach(() => {
    prepareSidebarDefaultMountContext({ storageAccessor, fixtures, vi })
  })

  afterEach(() => {
    teardownSidebarComponentTest(wrapper)
  })

  async function mountLayoutAtNote(noteId: number) {
    const router = createRouter({ history: createWebHistory(), routes })
    await router.push(noteShowLocation(noteId))
    wrapper = helper
      .component(NotebookSidebarLayout)
      .withRouter(router)
      .withCurrentUser(makeMe.aUser.please())
      .mount({ attachTo: document.body })
    await flushPromises()
    return router
  }

  it("keeps sidebar chrome for an uncached same-notebook note, then clears active on leave", async () => {
    const uncachedNote = uncachedNoteInSameNotebook(
      fixtures.topNoteRealm.notebookRealm,
      "uncached"
    )
    stubShowNotePending()
    mockSdkServiceWithImplementation(NotebookController, "get", () =>
      neverResolving()
    )

    const router = await mountLayoutAtNote(fixtures.firstGeneration.id)
    const activeTitle = fixtures.firstGeneration.note.noteTopology.title

    expect(sidebarShowsActiveItem(wrapper, activeTitle)).toBe(true)
    expect(wrapper.text()).toContain(notebookName)

    await router.push(noteShowLocation(uncachedNote.id))
    await flushPromises()

    expect(sidebarShowsActiveItem(wrapper, activeTitle)).toBe(true)
    expect(wrapper.text()).toContain(notebookName)

    await router.push({
      name: "notebookPage",
      params: {
        notebookId: String(fixtures.topNoteRealm.notebookRealm.notebook.id),
      },
    })
    await flushPromises()

    expect(sidebarShowsActiveItem(wrapper, activeTitle)).toBe(false)
  })

  it("closes the mobile drawer when navigating to another note", async () => {
    const originalWidth = window.innerWidth
    Object.defineProperty(window, "innerWidth", {
      configurable: true,
      value: 500,
    })
    try {
      const router = await mountLayoutAtNote(fixtures.firstGeneration.id)
      await wrapper.get('[aria-label="Show sidebar"]').trigger("click")
      expect(wrapper.find('[aria-label="Hide sidebar"]').exists()).toBe(true)

      await router.push(noteShowLocation(fixtures.firstGenerationSibling.id))
      await flushPromises()

      expect(wrapper.find('[aria-label="Show sidebar"]').exists()).toBe(true)
    } finally {
      Object.defineProperty(window, "innerWidth", {
        configurable: true,
        value: originalWidth,
      })
    }
  })
})
