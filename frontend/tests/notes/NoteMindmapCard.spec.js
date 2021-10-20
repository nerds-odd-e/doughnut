/**
 * @jest-environment jsdom
 */
import { mount } from "@vue/test-utils";
import NoteCard from "@/components/notes/mindmap/NoteCard.vue";
import MindmapSector from "@/models/MindmapSector";
import makeMe from "../fixtures/makeMe";

describe("note mindmap", () => {

  it("should render one note", async () => {
    const note =makeMe.aNote.title("single note").shortDescription('not long').please()
    const wrapper = mount(
      NoteCard,
      { propsData: { note, scale: 1, mindmapSector: new MindmapSector(0, 0, 0, 0) } },
    );
    await wrapper.find("note-card").trigger("mousewheel", { clientX: 100, clientY: 200, deltaY: -2000})
    expect(wrapper.emitted()).not.toHaveProperty('zoom')
  });

})
