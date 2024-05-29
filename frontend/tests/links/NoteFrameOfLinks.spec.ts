import NoteFrameOfLinks from "@/components/notes/core/NoteFrameOfLinks.vue";
import { Note } from "@/generated/backend";
import makeMe from "../fixtures/makeMe";
import RenderingHelper from "../helpers/RenderingHelper";
import helper from "../helpers";

describe("a link lists of a note", () => {
  let renderer: RenderingHelper;

  beforeEach(() => {
    renderer = helper.component(NoteFrameOfLinks);
  });

  it("link to upper level", async () => {
    const links = makeMe.linksMap.of(Note.linkType.USING).count(2).please();
    const wrapper = renderer.withStorageProps({ links }).mount();
    expect(wrapper.find(".parent-links").text()).toContain("target note");
    expect(wrapper.findAll(".parent-links li").length).toEqual(2);
  });

  it("tags are grouped", async () => {
    const links = makeMe.linksMap.of(Note.linkType.TAGGED_BY).count(2).please();
    const wrapper = renderer.withStorageProps({ links }).mount();
    expect(wrapper.findAll(".parent-links li").length).toEqual(1);
  });

  it("related, opposite, similar, confuse are grouped at top", async () => {
    const links = makeMe.linksMap
      .of(Note.linkType.CONFUSED_WITH)
      .and.of(Note.linkType.SIMILAR_TO)
      .please();
    const wrapper = renderer.withStorageProps({ links }).mount();
    expect(wrapper.findAll(".parent-links li").length).toEqual(1);
    expect(wrapper.findAll(".parent-links li .link-multi").length).toEqual(2);
    expect(wrapper.findAll(".children-links li").length).toEqual(0);
  });

  it("taggings (reverse of tagged by) are grouped", async () => {
    const links = makeMe.linksMap
      .of(Note.linkType.TAGGED_BY)
      .reverse.count(2)
      .please();
    const wrapper = renderer.withStorageProps({ links }).mount();
    expect(wrapper.findAll(".children-links li").length).toEqual(1);
  });
});
