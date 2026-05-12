import { useStorageAccessor } from "@/composables/useStorageAccessor"
import { NOTE_SIDEBAR_PEER_SORT_STORAGE_KEY } from "@/composables/useNoteSidebarPeerSort"
import createNoteStorage from "@/store/createNoteStorage"
import makeMe from "doughnut-test-fixtures/makeMe"
import helper from "@tests/helpers"
import { flushPromises } from "@vue/test-utils"
import { vi, describe, it, expect, beforeEach, afterEach } from "vitest"
import { sidebarDefaultTreeFixtures } from "./sidebarDefaultTree"
import {
  DEFAULT_ROOT_PEER_ORDER,
  mountSidebarSignedIn,
  prepareSidebarDefaultMountContext,
  rootRowLabels,
  setupRootPeersWithFolders,
  teardownSidebarComponentTest,
} from "./sidebarTestSupport"

describe("Sidebar peer sort", () => {
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

  function zebraApplePeerRealms(updates?: { z?: string; a?: string }) {
    const z = makeMe.aNoteRealm.title("zebra").under(fixtures.topNoteRealm)
    const a = makeMe.aNoteRealm.title("apple").under(fixtures.topNoteRealm)
    return {
      realmZ: (updates?.z ? z.updatedAt(updates.z) : z).please(),
      realmA: (updates?.a ? a.updatedAt(updates.a) : a).please(),
    }
  }

  async function flushUntilTwoRootFolderLabels() {
    await flushPromises()
    await vi.waitUntil(
      () => wrapper.findAll(".sidebar-folder-label").length >= 2
    )
  }

  it("shows sort control in the sidebar toolbar", async () => {
    wrapper = mountSidebarSignedIn(
      helper,
      fixtures.topNoteRealm,
      fixtures.topNoteRealm.notebookRealm.notebook.id
    )
    await flushPromises()
    expect(wrapper.find("[data-note-sidebar-sort]").exists()).toBe(true)
  })

  it("lists folders above notes (A–Z) and reorders root peers when Title (Z–A) is chosen", async () => {
    storageAccessor.value = createNoteStorage()
    const { realmZ, realmA } = zebraApplePeerRealms()
    const { nbId, realmA: activeA } = setupRootPeersWithFolders({
      storageAccessor,
      topNoteRealm: fixtures.topNoteRealm,
      realmZ,
      realmA,
    })

    wrapper = mountSidebarSignedIn(helper, activeA, nbId)
    await flushUntilTwoRootFolderLabels()

    expect(rootRowLabels(wrapper)).toEqual([...DEFAULT_ROOT_PEER_ORDER])

    await wrapper.find("[data-note-sidebar-sort] summary").trigger("click")
    await flushPromises()
    await wrapper.find('button[title="Title (Z–A)"]').trigger("click")
    await flushPromises()

    expect(rootRowLabels(wrapper)).toEqual([
      "folder:mango",
      "folder:banana",
      "note:zebra",
      "note:apple",
    ])
  })

  it("applies sort from sessionStorage on mount without opening the menu", async () => {
    sessionStorage.setItem(
      NOTE_SIDEBAR_PEER_SORT_STORAGE_KEY,
      JSON.stringify({ field: "updated", direction: "desc" })
    )
    storageAccessor.value = createNoteStorage()
    const { realmZ, realmA } = zebraApplePeerRealms({
      z: "2015-06-01T00:00:00.000Z",
      a: "2020-01-01T00:00:00.000Z",
    })
    setupRootPeersWithFolders({
      storageAccessor,
      topNoteRealm: fixtures.topNoteRealm,
      realmZ,
      realmA,
      folderExtras: {
        banana: { updatedAt: "2018-01-01T00:00:00.000Z" },
        mango: { updatedAt: "2005-01-01T00:00:00.000Z" },
      },
    })

    wrapper = mountSidebarSignedIn(
      helper,
      realmA,
      fixtures.topNoteRealm.notebookRealm.notebook.id
    )
    await flushUntilTwoRootFolderLabels()

    expect(rootRowLabels(wrapper)).toEqual([...DEFAULT_ROOT_PEER_ORDER])
  })
})
