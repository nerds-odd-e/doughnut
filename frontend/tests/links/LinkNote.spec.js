import LinkNote from '@/components/links/LinkNote.vue';
import { mountWithMockRoute } from '../helpers'

describe('LinkNote', () => {
  beforeEach(async () => {
    fetch.resetMocks();
    fetch.mockResponseOnce(JSON.stringify({}));
  });

    test('click on note when doing review', async () => {
      const { wrapper } = mountWithMockRoute(LinkNote, {propsData: reviewPointForView}, {name: 'repeat'});

      expect(JSON.parse(wrapper.find(".link-source .router-link").attributes().to).name).toEqual("noteShow")
    });

    test('click on note when doing review and in a nested page', async () => {
      const { wrapper } = mountWithMockRoute(Repetition, {propsData: reviewPointForView}, {name: 'repeat-noteShow', params: {noteid: 123}});
      expect(JSON.parse(wrapper.find(".link-source .router-link").attributes().to)).toEqual({name: 'noteShow', params: {noteid: 1}})
    });

    test('compact view', async () => {
      const { wrapper, mockRouter } = mountWithMockRoute(Repetition, {propsData: {...reviewPointForView, compact: true}}, {name: 'repeat-noteShow', params: {noteid: 123}});
      expect(wrapper.find(".link-source .router-link").exists()).toBe(false)
      await wrapper.find(".repeat-container").trigger('click')
      expect(mockRouter.push).toHaveBeenCalledWith({name: 'repeat'})
    });

  });

});
