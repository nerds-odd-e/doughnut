import _ from "lodash";
import makeMe from "../fixtures/makeMe.ts";
import NoteFrameOfLinks from "@/components/links/NoteFrameOfLinks.vue";
import { staticInfo } from "../notes/fixtures-basic";
import { mountWithMockRoute } from "../helpers";

describe("a link lists of a note", () => {
  test("link to upper level", async () => {
    const links = makeMe.links.of("using").count(2).please();
    const { wrapper } = mountWithMockRoute(NoteFrameOfLinks, {
      propsData: { links, owns: true, staticInfo },
    });
    expect(wrapper.find(".parent-links").text()).toContain("a tool");
    expect(wrapper.findAll(".parent-links li").length).toEqual(2);
  });

  test("tags are grouped", async () => {
    const links = makeMe.links.of("tagged by").count(2).please();
    const { wrapper } = mountWithMockRoute(NoteFrameOfLinks, {
      propsData: { links, owns: true, staticInfo },
    });
    expect(wrapper.findAll(".parent-links li").length).toEqual(1);
  });

  test("related, opposite, similar, confuse are grouped at top", async () => {
    const links = makeMe.links.of('confused with').and.of('similar to').please();
    const { wrapper } = mountWithMockRoute(NoteFrameOfLinks, {
      propsData: { links, owns: true, staticInfo },
    });
    expect(wrapper.findAll(".parent-links li").length).toEqual(1);
    expect(wrapper.findAll(".parent-links li .link-multi").length).toEqual(2);
    expect(wrapper.findAll(".children-links li").length).toEqual(0);
  });

  test("taggings are grouped", async () => {
    const links = makeMe.links.of("tagged by").reverse.count(2).please();
    const { wrapper } = mountWithMockRoute(NoteFrameOfLinks, {
      propsData: { links, owns: true, staticInfo },
    });
    expect(wrapper.findAll(".children-links li").length).toEqual(1);
  });

});
