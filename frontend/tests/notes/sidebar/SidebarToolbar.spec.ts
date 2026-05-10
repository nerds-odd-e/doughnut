import { useStorageAccessor } from "@/composables/useStorageAccessor"
import type { NoteRealm } from "@generated/doughnut-backend-api"
import helper from "@tests/helpers"
import { flushPromises } from "@vue/test-utils"
import { vi, describe, it, expect, beforeEach, afterEach } from "vitest"
import { sidebarDefaultTreeFixtures } from "./sidebarDefaultTree"
import {
  mountSidebar,
  mountSidebarSignedIn,
  prepareSidebarDefaultMountContext,
  teardownSidebarComponentTest,
} from "./sidebarTestSupport"

describe("Sidebar toolbar", () => {
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

  it("shows New note and New folder when signed in and notebook is not from bazaar", async () => {
    wrapper = mountSidebarSignedIn(
      helper,
      fixtures.topNoteRealm,
      fixtures.topNoteRealm.notebookView.notebook.id
    )
    await flushPromises()
    expect(wrapper.find('button[title="New note"]').exists()).toBe(true)
    expect(wrapper.find('button[title="New folder"]').exists()).toBe(true)
  })

  it("hides New folder when no current user", async () => {
    wrapper = mountSidebar(helper, fixtures.firstGeneration)
    await flushPromises()
    expect(wrapper.find('button[title="New folder"]').exists()).toBe(false)
  })

  it("hides New folder when note realm is from bazaar", async () => {
    const bazaarRealm = {
      ...fixtures.firstGeneration,
      notebookView: {
        ...fixtures.firstGeneration.notebookView,
        readonly: true,
      },
    } as NoteRealm
    wrapper = mountSidebarSignedIn(
      helper,
      bazaarRealm,
      bazaarRealm.notebookView.notebook.id
    )
    await flushPromises()
    expect(wrapper.find('button[title="New folder"]').exists()).toBe(false)
  })
})
