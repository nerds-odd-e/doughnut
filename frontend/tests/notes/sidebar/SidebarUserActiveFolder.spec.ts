import { useStorageAccessor } from "@/composables/useStorageAccessor"
import { DOMWrapper, flushPromises } from "@vue/test-utils"
import { vi, describe, it, expect, beforeEach, afterEach } from "vitest"
import helper from "@tests/helpers"
import { sidebarDefaultTreeFixtures } from "./sidebarDefaultTree"
import {
  findSidebarItem,
  mountSidebar,
  mountSidebarSignedIn,
  prepareSidebarDefaultMountContext,
  teardownSidebarComponentTest,
} from "./sidebarTestSupport"

describe("Sidebar user active folder", () => {
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

  async function mountFirstGenSidebarAndWaitNoteRow() {
    wrapper = mountSidebar(helper, fixtures.firstGeneration)
    await vi.waitUntil(() =>
      findSidebarItem(
        wrapper,
        fixtures.firstGeneration.note.noteTopology.title
      )?.exists()
    )
  }

  async function mountSignedInFirstGenWithRootFolderUserActive() {
    wrapper = mountSidebarSignedIn(
      helper,
      fixtures.firstGeneration,
      fixtures.firstGeneration.notebookView.notebook.id
    )
    await flushPromises()
    await vi.waitUntil(() =>
      findSidebarItem(
        wrapper,
        fixtures.firstGeneration.note.noteTopology.title
      )?.exists()
    )
    const folderRow = findRootFolderRowByTopTitle()
    expect(folderRow?.exists()).toBe(true)
    await folderRow!.find(".sidebar-folder-label").trigger("click")
    await flushPromises()
    expect(folderRow!.classes()).toContain("sidebar-folder-user-active")
    return folderRow!
  }

  it("applies sidebar-folder-user-active when a folder row is clicked", async () => {
    await mountFirstGenSidebarAndWaitNoteRow()
    const folderRow = findRootFolderRowByTopTitle()
    expect(folderRow?.exists()).toBe(true)
    await folderRow!.find(".sidebar-folder-label").trigger("click")
    await flushPromises()
    expect(folderRow!.classes()).toContain("sidebar-folder-user-active")
  })

  it("activates folder and toggles expand when the folder label area is clicked", async () => {
    await mountFirstGenSidebarAndWaitNoteRow()
    const folderRow = findRootFolderRowByTopTitle()
    expect(folderRow?.exists()).toBe(true)
    const expandedBefore = folderRow!.attributes("aria-expanded")
    const track = folderRow!.find(".folder-label-area")
    expect(track.exists()).toBe(true)
    await track.trigger("click")
    await flushPromises()
    expect(folderRow!.classes()).toContain("sidebar-folder-user-active")
    expect(folderRow!.attributes("aria-expanded")).not.toBe(expandedBefore)
  })

  it("clears user active folder styling when a note row is clicked", async () => {
    wrapper = mountSidebar(helper, fixtures.firstGeneration)
    await vi.waitUntil(() =>
      findSidebarItem(
        wrapper,
        fixtures.topNoteRealm.note.noteTopology.title
      )?.exists()
    )
    const folderRow = findRootFolderRowByTopTitle()
    await folderRow!.find(".sidebar-folder-label").trigger("click")
    await flushPromises()
    expect(folderRow!.classes()).toContain("sidebar-folder-user-active")

    const noteRow = findSidebarItem(
      wrapper,
      fixtures.topNoteRealm.note.noteTopology.title
    )
    expect(noteRow?.exists()).toBe(true)
    await noteRow!.trigger("click")
    await flushPromises()
    expect(folderRow!.classes()).not.toContain("sidebar-folder-user-active")
  })

  it("does not clear user active folder when modal opens after toolbar click (Safari behavior)", async () => {
    const folderRow = await mountSignedInFirstGenWithRootFolderUserActive()

    const toolbarBtn = wrapper.find('button[title="New note"]')
    expect(toolbarBtn.exists()).toBe(true)
    toolbarBtn.element.dispatchEvent(
      new MouseEvent("mousedown", { bubbles: true })
    )
    const modalInput = document.createElement("input")
    document.body.appendChild(modalInput)
    folderRow!.element.focus()
    folderRow!.element.dispatchEvent(
      new FocusEvent("focusout", {
        bubbles: true,
        relatedTarget: modalInput,
      })
    )
    await flushPromises()
    document.body.removeChild(modalInput)
    expect(folderRow!.classes()).toContain("sidebar-folder-user-active")
  })

  it("does not clear user active folder when focus moves to the sidebar toolbar", async () => {
    const folderRow = await mountSignedInFirstGenWithRootFolderUserActive()
    const toolbarBtn = wrapper.find('button[title="New folder"]')
    expect(toolbarBtn.exists()).toBe(true)
    folderRow!.element.focus()
    folderRow!.element.dispatchEvent(
      new FocusEvent("focusout", {
        bubbles: true,
        relatedTarget: toolbarBtn.element,
      })
    )
    await flushPromises()
    expect(folderRow!.classes()).toContain("sidebar-folder-user-active")
  })
})
