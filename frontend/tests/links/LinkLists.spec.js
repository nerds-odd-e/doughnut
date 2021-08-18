import LinkLists from '@/components/links/LinkLists.vue';
import {basicNote} from '../notes/fixtures-basic'
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
    const { wrapper } = mountWithMockRoute(LinkLists, {propsData: { links, owns: true, colors: {} }})
    expect(wrapper.find(".link-title").text()).toContain("a tool")
  });

});
