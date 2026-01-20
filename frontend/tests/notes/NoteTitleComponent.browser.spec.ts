import NoteTitleComponent from "@/components/notes/core/NoteTitleComponent.vue"
import type { Note } from "@generated/backend"
import { type VueWrapper, flushPromises } from "@vue/test-utils"
import type { ComponentPublicInstance } from "vue"
import makeMe from "@tests/fixtures/makeMe"
import helper from "@tests/helpers"
import { describe, it, expect, beforeEach, afterEach, vi } from "vitest"

describe("note title", () => {
  // biome-ignore lint/suspicious/noExplicitAny: wrapper for testing
  let wrapper: VueWrapper<any>

  const mountComponent = (n: Note): VueWrapper<ComponentPublicInstance> => {
    wrapper = helper
      .component(NoteTitleComponent)
      .withProps({
        noteTopology: n.noteTopology,
      })
      .mount({ attachTo: document.body })
    return wrapper
  }

  beforeEach(() => {
    vi.resetAllMocks()
  })

  afterEach(() => {
    wrapper?.unmount()
    document.body.innerHTML = ""
  })

  describe("relationship note", () => {
    const note = makeMe.aNote.title("Dummy Title").please()
    const target = makeMe.aNote.underNote(note).please()
    const relationNote = makeMe.aRelationship.to(target).please()

    it("should have relationship to target", async () => {
      mountComponent(relationNote)
      await flushPromises()
      const links = wrapper.findAll("a.router-link")
      const link = links[0]!
      expect(link.exists()).toBe(true)
      expect(JSON.parse(link.attributes("to")!)).toMatchObject({
        name: "noteShow",
        params: { noteId: target.id },
      })
      expect(link.text()).toBe(target.noteTopology.title!)
    })

    it("if relationship note has details the relationship is an icon", async () => {
      relationNote.noteTopology.shortDetails = "exist"
      mountComponent(relationNote)
      await flushPromises()
      const link = wrapper.find("a.router-link")
      expect(link.exists()).toBe(true)
      expect(link.text()).toBe("🔗")
      expect(wrapper.text()).toContain(target.noteTopology.title!)
    })
  })
})
