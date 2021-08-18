import LinkLists from '@/components/links/LinkLists.vue';
import {basicNote, staticInfo} from '../notes/fixtures-basic'
import _ from 'lodash'
import { mountWithMockRoute } from '../helpers'

describe('a link lists of a note', () => {

  test('link to upper level', async () => {
    const links = {
      "using": {
        "direct": [
          {
            "id": 1938,
            "targetNote": {
              "id": 2423,
              "title": "a tool",
            },
            "typeId": 15,
            "linkTypeLabel": "using",
            "linkNameOfSource": "user"
          }
        ],
        "reverse": []
      },
    }
    const { wrapper } = mountWithMockRoute(LinkLists, {propsData: { links, owns: true, staticInfo }})
    expect(wrapper.find(".parent-links").text()).toContain("a tool")
  });

  test('tags are grouped', async () => {
    const links = {
      "tagged by": {
        "direct": [
          {
            "id": 1938,
            "targetNote": {
              "id": 2423,
              "title": "tag1",
            },
            "typeId": 8,
            "linkTypeLabel": "tagged by",
            "linkNameOfSource": "user"
          },
          {
            "id": 1938,
            "targetNote": {
              "id": 2423,
              "title": "tag2",
            },
            "typeId": 8,
            "linkTypeLabel": "tagged by",
            "linkNameOfSource": "user"
          }
        ],
        "reverse": []
      },
    }
    const { wrapper } = mountWithMockRoute(LinkLists, {propsData: { links, owns: true, staticInfo: staticInfo }})
    expect(wrapper.findAll(".parent-links li").length).toEqual(1)
  });

});
