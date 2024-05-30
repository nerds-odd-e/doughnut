import NoteFrameOfLinks from "@/components/notes/core/NoteFrameOfLinks.vue";
import { NoteTopic } from "@/generated/backend";
import makeMe from "../fixtures/makeMe";
import RenderingHelper from "../helpers/RenderingHelper";
import helper from "../helpers";

describe("a link lists of a note", () => {
  let renderer: RenderingHelper;

  beforeEach(() => {
    renderer = helper.component(NoteFrameOfLinks);
  });

  it("taggings (reverse of tagged by) are grouped", async () => {
    const links = makeMe.linksMap
      .of(NoteTopic.linkType.TAGGED_BY)
      .reverse.count(2)
      .please();
    const wrapper = renderer.withStorageProps({ links }).mount();
    expect(wrapper.findAll(".children-links li").length).toEqual(1);
  });
});
