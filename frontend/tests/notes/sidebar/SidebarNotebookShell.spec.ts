import { notebookSidebarNotebookClientView } from "@/composables/useCurrentNoteSidebarState"
import { useStorageAccessor } from "@/composables/useStorageAccessor"
import makeMe from "doughnut-test-fixtures/makeMe"
import helper from "@tests/helpers"
import { flushPromises } from "@vue/test-utils"
import { vi, describe, it, expect, beforeEach, afterEach } from "vitest"
import { sidebarDefaultTreeFixtures } from "./sidebarDefaultTree"
import {
  findSidebarItem,
  mountSidebarSignedIn,
  prepareSidebarDefaultMountContext,
  teardownSidebarComponentTest,
} from "./sidebarTestSupport"

describe("Sidebar notebook shell", () => {
  // biome-ignore lint/suspicious/noExplicitAny: wrapper for testing
  let wrapper: import("@vue/test-utils").VueWrapper<any>
  const storageAccessor = useStorageAccessor()
  const fixtures = sidebarDefaultTreeFixtures

  beforeEach(() => {
    prepareSidebarDefaultMountContext({
      storageAccessor,
      fixtures,
      vi,
    })
  })

  afterEach(() => {
    teardownSidebarComponentTest(wrapper)
  })

  it("shows folder expand control at notebook root with shallow folder listing", async () => {
    wrapper = mountSidebarSignedIn(
      helper,
      fixtures,
      undefined,
      fixtures.topNoteRealm.notebookView.notebook.id
    )
    await flushPromises()
    expect(wrapper.find('button[aria-label="expand children"]').exists()).toBe(
      true
    )
  })

  it("shows notebook root notes and add button when anchor realm is cleared on notebook page", async () => {
    wrapper = mountSidebarSignedIn(
      helper,
      fixtures,
      fixtures.topNoteRealm,
      fixtures.topNoteRealm.notebookView.notebook.id
    )
    await flushPromises()
    await wrapper.setProps({
      activeNoteRealm: undefined,
      notebookId: fixtures.topNoteRealm.notebookView.notebook.id,
    })
    await flushPromises()
    expect(
      findSidebarItem(
        wrapper,
        fixtures.topNoteRealm.note.noteTopology.title
      )?.exists()
    ).toBe(true)
    expect(wrapper.find('button[title="New note"]').exists()).toBe(true)
  })

  it("hides New note when notebook page is readonly and anchor realm is not loaded yet", async () => {
    notebookSidebarNotebookClientView.value = {
      notebook: makeMe.aNotebook.please(),
      readonly: true,
    }
    wrapper = mountSidebarSignedIn(
      helper,
      fixtures,
      undefined,
      fixtures.topNoteRealm.notebookView.notebook.id
    )
    await flushPromises()
    expect(wrapper.find('button[title="New note"]').exists()).toBe(false)
  })
})
