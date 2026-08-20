import { useStorageAccessor } from "@/composables/useStorageAccessor"
import { PEER_SORT_STORAGE_KEY } from "@/composables/usePeerSort"
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

  const titleZaRootOrder = [
    "folder:mango",
    "folder:banana",
    "note:zebra",
    "note:apple",
  ]

  function zebraApplePeerRealms() {
    return {
      realmZ: makeMe.aNoteRealm
        .title("zebra")
        .under(fixtures.topNoteRealm)
        .please(),
      realmA: makeMe.aNoteRealm
        .title("apple")
        .under(fixtures.topNoteRealm)
        .please(),
    }
  }

  async function flushUntilTwoRootFolderLabels() {
    await flushPromises()
    await vi.waitUntil(
      () => wrapper.findAll(".sidebar-folder-label").length >= 2
    )
  }

  async function mountZebraAppleRootSidebar() {
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
    return { activeA, nbId }
  }

  async function chooseTitleZa() {
    await wrapper.find("[data-note-sidebar-sort] summary").trigger("click")
    await flushPromises()
    const titleZaButton = document.querySelector('button[title="Title (Z–A)"]')
    expect(titleZaButton).not.toBeNull()
    await (titleZaButton as HTMLButtonElement).click()
    await flushPromises()
  }

  it("lists folders above notes (A–Z) and reorders root peers when Title (Z–A) is chosen", async () => {
    await mountZebraAppleRootSidebar()

    expect(wrapper.find("[data-note-sidebar-sort]").exists()).toBe(true)
    expect(rootRowLabels(wrapper)).toEqual([...DEFAULT_ROOT_PEER_ORDER])

    await chooseTitleZa()

    expect(rootRowLabels(wrapper)).toEqual(titleZaRootOrder)
  })

  it("keeps Title (Z–A) on a later visit after the tab session is gone", async () => {
    const { activeA, nbId } = await mountZebraAppleRootSidebar()
    await chooseTitleZa()

    wrapper.unmount()
    sessionStorage.removeItem(PEER_SORT_STORAGE_KEY)

    wrapper = mountSidebarSignedIn(helper, activeA, nbId)
    await flushUntilTwoRootFolderLabels()

    expect(rootRowLabels(wrapper)).toEqual(titleZaRootOrder)
  })
})
