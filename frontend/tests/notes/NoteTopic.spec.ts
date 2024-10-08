import NoteTopicComponent from "@/components/notes/core/NoteTopicComponent.vue"
import type { Note } from "@/generated/backend"
import { VueWrapper } from "@vue/test-utils"
import type { ComponentPublicInstance } from "vue"
import makeMe from "../fixtures/makeMe"
import helper from "../helpers"

describe("note topic", () => {
  const mountComponent = (n: Note): VueWrapper<ComponentPublicInstance> => {
    return helper
      .component(NoteTopicComponent)
      .withProps({
        noteTopic: n.noteTopic,
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
      const link = wrapper.find("a.router-link")
      expect(link.exists()).toBe(true)
      expect(JSON.parse(link.attributes("to")!)).toMatchObject({
        name: "noteShow",
        params: { noteId: target.id },
      })
      expect(link.text()).toBe(target.noteTopic.topicConstructor)
    })

    it("if linking note has details the link is an icon", async () => {
      linkingNote.noteTopic.shortDetails = "exist"
      const wrapper = mountComponent(linkingNote)
      const link = wrapper.find("a.router-link")
      expect(link.exists()).toBe(true)
      expect(link.text()).toBe("🔗")
      expect(wrapper.text()).toContain(target.noteTopic.topicConstructor)
    })
  })
})
