import NoteTitleComponent from "@/components/notes/core/NoteTitleComponent.vue"
import type { Note } from "@generated/doughnut-backend-api"
import { type VueWrapper, flushPromises } from "@vue/test-utils"
import type { ComponentPublicInstance } from "vue"
import makeMe from "doughnut-test-fixtures/makeMe"
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

    it("renders stored title as plain text without links", async () => {
      mountComponent(relationNote)
      await flushPromises()
      expect(wrapper.find(".title-text").text().trim()).toBe(
        relationNote.noteTopology.title!.trim()
      )
      expect(wrapper.findAll("a").length).toBe(0)
    })
  })
})
