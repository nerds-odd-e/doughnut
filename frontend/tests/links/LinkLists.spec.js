import _ from "lodash";
import LinkLists from "@/components/links/LinkLists.vue";
import { staticInfo } from "../notes/fixtures-basic";
import { mountWithMockRoute } from "../helpers";

describe("a link lists of a note", () => {
  test("link to upper level", async () => {
    const links = {
      using: {
        direct: [
          {
            id: 1938,
            targetNote: {
              id: 2423,
              title: "a tool",
            },
            typeId: 15,
            linkTypeLabel: "using",
            linkNameOfSource: "user",
          },
          {
            id: 1938,
            targetNote: {
              id: 2423,
              title: "another tool",
            },
            typeId: 15,
            linkTypeLabel: "using",
            linkNameOfSource: "user",
          },
        ],
        reverse: [],
      },
    };
    const { wrapper } = mountWithMockRoute(LinkLists, {
      propsData: { links, owns: true, staticInfo },
    });
    expect(wrapper.find(".parent-links").text()).toContain("a tool");
    expect(wrapper.findAll(".parent-links li").length).toEqual(2);
  });

  test("tags are grouped", async () => {
    const links = {
      "tagged by": {
        direct: [
          {
            id: 1938,
            targetNote: {
              id: 2423,
              title: "tag1",
            },
            typeId: 8,
            linkTypeLabel: "tagged by",
            linkNameOfSource: "user",
          },
          {
            id: 1938,
            targetNote: {
              id: 2423,
              title: "tag2",
            },
            typeId: 8,
            linkTypeLabel: "tagged by",
            linkNameOfSource: "user",
          },
        ],
        reverse: [],
      },
    };
    const { wrapper } = mountWithMockRoute(LinkLists, {
      propsData: { links, owns: true, staticInfo },
    });
    expect(wrapper.findAll(".parent-links li").length).toEqual(1);
  });

  test("related, opposite, similar, confuse are grouped at top", async () => {
    const links = {
      "confused with": {
        direct: [
          {
            id: 1938,
            targetNote: {
              id: 2423,
              title: "tag1",
            },
            typeId: 8,
            linkTypeLabel: "confusing",
            linkNameOfSource: "user",
          },
        ],
        reverse: [],
      },

      "similar to": {
        reverse: [
          {
            id: 1938,
            targetNote: {
              id: 2423,
              title: "something similar",
            },
            typeId: 8,
            linkTypeLabel: "tagged by",
            linkNameOfSource: "user",
          },
        ],
        direct: [],
      },
    };
    const { wrapper } = mountWithMockRoute(LinkLists, {
      propsData: { links, owns: true, staticInfo: staticInfo },
    });
    expect(wrapper.findAll(".parent-links li").length).toEqual(1);
    expect(wrapper.findAll(".parent-links li .link-multi").length).toEqual(2);
    expect(wrapper.findAll(".children-links li").length).toEqual(0);
  });
});
