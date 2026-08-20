import { useStorageAccessor } from "@/composables/useStorageAccessor"
import helper from "@tests/helpers"
import { vi, describe, it, expect, beforeEach, afterEach } from "vitest"
import { sidebarDefaultTreeFixtures } from "./sidebarDefaultTree"
import {
  findSidebarItem,
  isBefore,
  mountSidebarFirstGenReady,
  mountSidebarNotesReady,
  prepareSidebarDefaultMountContext,
  stubIntersectionObserver,
  teardownSidebarComponentTest,
} from "./sidebarTestSupport"

describe("Sidebar first generation", () => {
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

  it("shows the active note without scrolling when already intersecting", async () => {
    const restoreIntersectionObserver = stubIntersectionObserver(true)

    wrapper = await mountSidebarFirstGenReady(helper, fixtures)
    const activeElement = wrapper.find(".active-item")
    expect(activeElement.exists()).toBe(true)
    expect(activeElement.text()).toContain(
      fixtures.firstGeneration.note.noteTopology.title
    )
    expect(HTMLElement.prototype.scrollIntoView).not.toHaveBeenCalled()
    restoreIntersectionObserver()
  })

  it("orders nested child note before same-folder sibling when deeper note is active", async () => {
    wrapper = await mountSidebarNotesReady(helper, fixtures.secondGeneration, [
      fixtures.secondGeneration.note.noteTopology.title,
      fixtures.firstGenerationSibling.note.noteTopology.title,
    ])

    const secondGen = findSidebarItem(
      wrapper,
      fixtures.secondGeneration.note.noteTopology.title
    )!.element
    const sibling = findSidebarItem(
      wrapper,
      fixtures.firstGenerationSibling.note.noteTopology.title
    )!.element
    expect(isBefore(secondGen, sibling)).toBe(true)
  })
})
