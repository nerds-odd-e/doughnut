import NoteTitleComponent from "@/components/notes/core/NoteTitleComponent.vue"
import type { Note } from "generated/backend"
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

  describe("linking note", () => {
    const note = makeMe.aNote.topicConstructor("Dummy Title").please()
    const target = makeMe.aNote.underNote(note).please()
    const linkingNote = makeMe.aLink.to(target).please()

    it("should have link to target", async () => {
      const wrapper = mountComponent(linkingNote)
      const links = wrapper.findAll("a.router-link")
      const link = links[0]!
      expect(link.exists()).toBe(true)
      expect(JSON.parse(link.attributes("to")!)).toMatchObject({
        name: "noteShow",
        params: { noteId: target.id },
      })
      expect(link.text()).toBe(target.noteTopology.titleOrPredicate)
    })

    it("if linking note has details the link is an icon", async () => {
      linkingNote.noteTopology.shortDetails = "exist"
      const wrapper = mountComponent(linkingNote)
      const link = wrapper.find("a.router-link")
      expect(link.exists()).toBe(true)
      expect(link.text()).toBe("🔗")
      expect(wrapper.text()).toContain(target.noteTopology.titleOrPredicate)
    })
  })
})
