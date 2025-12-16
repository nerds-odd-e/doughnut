import NoteTitleComponent from "@/components/notes/core/NoteTitleComponent.vue"
import type { Note } from "@generated/backend"
import { VueWrapper } from "@vue/test-utils"
import type { ComponentPublicInstance } from "vue"
import makeMe from "@tests/fixtures/makeMe"
import helper from "@tests/helpers"

describe("note title", () => {
  const mountComponent = (n: Note): VueWrapper<ComponentPublicInstance> => {
    return helper
      .component(NoteTitleComponent)
      .withProps({
        noteTopology: n.noteTopology,
      })
      .mount()
  }

  beforeEach(() => {
    vi.resetAllMocks()
  })

  describe("relationship note", () => {
    const note = makeMe.aNote.title("Dummy Title").please()
    const target = makeMe.aNote.underNote(note).please()
    const relationNote = makeMe.aRelationship.to(target).please()

    it("should have relationship to target", async () => {
      const wrapper = mountComponent(relationNote)
      const links = wrapper.findAll("a.router-link")
      const link = links[0]!
      expect(link.exists()).toBe(true)
      expect(JSON.parse(link.attributes("to")!)).toMatchObject({
        name: "noteShow",
        params: { noteId: target.id },
      })
      expect(link.text()).toBe(target.noteTopology.titleOrPredicate)
    })

    it("if relationship note has details the relationship is an icon", async () => {
      relationNote.noteTopology.shortDetails = "exist"
      const wrapper = mountComponent(relationNote)
      const link = wrapper.find("a.router-link")
      expect(link.exists()).toBe(true)
      expect(link.text()).toBe("🔗")
      expect(wrapper.text()).toContain(target.noteTopology.titleOrPredicate)
    })
  })
})
