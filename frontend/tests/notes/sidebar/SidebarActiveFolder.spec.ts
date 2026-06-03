import { useStorageAccessor } from "@/composables/useStorageAccessor"
import { DOMWrapper, flushPromises } from "@vue/test-utils"
import { vi, describe, it, expect, beforeEach, afterEach } from "vitest"
import helper from "@tests/helpers"
import { sidebarDefaultTreeFixtures } from "./sidebarDefaultTree"
import {
  FOLDER_TOP_NOTE_CHILDREN_ID,
  mountSidebarFirstGenReady,
  prepareSidebarDefaultMountContext,
  teardownSidebarComponentTest,
} from "./sidebarTestSupport"

describe("Sidebar active folder", () => {
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

  const findRootFolderRowByTopTitle = () => {
    const label = wrapper
      .findAll(".sidebar-folder-label")
      .find((w) =>
        w.text().includes(fixtures.topNoteRealm.note.noteTopology.title)
      )
    const li = label?.element.closest("li")
    return li != null ? new DOMWrapper(li) : undefined
  }

  it("navigates to folderPage when the folder label is clicked", async () => {
    wrapper = await mountSidebarFirstGenReady(helper, fixtures)
    const folderRow = findRootFolderRowByTopTitle()
    expect(folderRow?.exists()).toBe(true)
    const router = wrapper.vm.$router
    const pushSpy = vi.spyOn(router, "push").mockResolvedValue(undefined)
    await folderRow!.find(".sidebar-folder-label").trigger("click")
    await flushPromises()
    expect(pushSpy).toHaveBeenCalledWith({
      name: "folderPage",
      params: {
        notebookId: String(fixtures.firstGeneration.notebookRealm.notebook.id),
        folderId: String(FOLDER_TOP_NOTE_CHILDREN_ID),
      },
    })
  })

  it("activates folder and toggles expand when the folder label area is clicked", async () => {
    wrapper = await mountSidebarFirstGenReady(helper, fixtures)
    const folderRow = findRootFolderRowByTopTitle()
    expect(folderRow?.exists()).toBe(true)
    const expandedBefore = folderRow!.attributes("aria-expanded")
    const track = folderRow!.find(".folder-label-area")
    expect(track.exists()).toBe(true)
    await track.trigger("click")
    await flushPromises()
    expect(folderRow!.attributes("aria-expanded")).not.toBe(expandedBefore)
  })
})
