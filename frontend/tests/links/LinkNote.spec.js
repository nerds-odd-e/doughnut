import LinkNote from '@/components/links/LinkNote.vue';
import { mountWithMockRoute } from '../helpers'

describe('LinkNote', () => {
  beforeEach(async () => {
    fetch.resetMocks();
    fetch.mockResponseOnce(JSON.stringify({}));
  });

  xtest('click on note when doing review', async () => {
    const { wrapper } = mountWithMockRoute(LinkNote, {propsData: reviewPointForView}, {name: 'repeat'});

    expect(JSON.parse(wrapper.find(".link-source .router-link").attributes().to).name).toEqual("noteShow")
  });

});
