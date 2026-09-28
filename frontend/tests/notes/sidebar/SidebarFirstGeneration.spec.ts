import { useNoteStore } from "@/store/noteStore"
import helper from "@tests/helpers"
import { vi, describe, it, expect, beforeEach, afterEach } from "vitest"
import { sidebarDefaultTreeFixtures } from "./sidebarDefaultTree"
import {
  findSidebarItem,
  isBefore,
  mountSidebarNotesReady,
  prepareSidebarDefaultMountContext,
  teardownSidebarComponentTest,
} from "./sidebarTestSupport"

describe("Sidebar first generation", () => {
  // biome-ignore lint/suspicious/noExplicitAny: wrapper for testing
  let wrapper: import("@vue/test-utils").VueWrapper<any>
  const noteStore = useNoteStore()
  const fixtures = sidebarDefaultTreeFixtures

  beforeEach(() => {
    prepareSidebarDefaultMountContext({
      noteStore,
      fixtures,
      vi,
    })
  })

  afterEach(() => {
    teardownSidebarComponentTest(wrapper)
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
