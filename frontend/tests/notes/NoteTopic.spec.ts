import { VueWrapper } from "@vue/test-utils";
import { ComponentPublicInstance } from "vue";
import { Note } from "@/generated/backend";
import NoteTopicComponent from "@/components/notes/core/NoteTopicComponent.vue";
import makeMe from "../fixtures/makeMe";
import helper from "../helpers";

describe("note topic", () => {
  const mountComponent = (n: Note): VueWrapper<ComponentPublicInstance> => {
    return helper
      .component(NoteTopicComponent)
      .withProps({
        noteTopic: n.noteTopic,
      })
      .mount();
  };

  beforeEach(() => {
    vi.resetAllMocks();
  });

  describe("linking note", () => {
    const note = makeMe.aNote.topicConstructor("Dummy Title").please();
    const target = makeMe.aNote.underNote(note).please();
    const linkingNote = makeMe.aLink.to(target).please();

    it("should have link to target", async () => {
      const wrapper = mountComponent(linkingNote);
      const link = wrapper.find("a.router-link");
      expect(link.exists()).toBe(true);
      expect(JSON.parse(link.attributes("to")!)).toMatchObject({
        name: "noteShow",
        params: { noteId: target.id },
      });
    });
  });
});
