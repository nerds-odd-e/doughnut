import { useStorageAccessor } from "@/composables/useStorageAccessor"
import helper from "@tests/helpers"
import { flushPromises } from "@vue/test-utils"
import { vi, describe, it, expect, beforeEach, afterEach } from "vitest"
import { sidebarDefaultTreeFixtures } from "./sidebarDefaultTree"
import {
  findSidebarItem,
  isBefore,
  mountSidebar,
  prepareSidebarDefaultMountContext,
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

  it("should scroll to active note", async () => {
    wrapper = mountSidebar(helper, fixtures, fixtures.firstGeneration)
    await flushPromises()
    await new Promise((resolve) =>
      requestAnimationFrame(() => resolve(undefined))
    )
    await vi.waitUntil(() =>
      findSidebarItem(
        wrapper,
        fixtures.firstGeneration.note.noteTopology.title
      )?.exists()
    )
    const activeElement = wrapper.find(".active-item")
    expect(activeElement.exists()).toBe(true)
    expect(activeElement.text()).toContain(
      fixtures.firstGeneration.note.noteTopology.title
    )
  })

  it("should not scroll if already visible", async () => {
    const originalIntersectionObserver = window.IntersectionObserver
    window.IntersectionObserver = class extends originalIntersectionObserver {
      constructor(callback: IntersectionObserverCallback) {
        super(callback)
        setTimeout(() => {
          callback(
            [{ isIntersecting: true }] as IntersectionObserverEntry[],
            this
          )
        }, 0)
      }
    } as typeof IntersectionObserver

    wrapper = mountSidebar(helper, fixtures, fixtures.firstGeneration)
    await flushPromises()
    await new Promise((resolve) =>
      requestAnimationFrame(() => resolve(undefined))
    )
    await flushPromises()
    expect(HTMLElement.prototype.scrollIntoView).not.toHaveBeenCalled()
    window.IntersectionObserver = originalIntersectionObserver
  })

  it("should have child note of active first gen", async () => {
    wrapper = mountSidebar(helper, fixtures, fixtures.firstGeneration)
    await flushPromises()
    await vi.waitUntil(() =>
      findSidebarItem(
        wrapper,
        fixtures.firstGenerationSibling.note.noteTopology.title
      )?.exists()
    )
    const firstTitle = fixtures.firstGeneration.note.noteTopology.title
    const nestedFolderLabel = wrapper
      .findAll(".sidebar-folder-label")
      .find((w) => w.text().trim() === firstTitle)
    expect(nestedFolderLabel?.exists()).toBe(true)
    await nestedFolderLabel!.trigger("click")
    await flushPromises()
    await vi.waitUntil(() =>
      findSidebarItem(
        wrapper,
        fixtures.secondGeneration.note.noteTopology.title
      )?.exists()
    )

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
