import NoteStatistics from '@/components/NoteStatistics.vue';
import { mount } from '@vue/test-utils';
import flushPromises from 'flush-promises';

beforeEach(() => {
  fetch.resetMocks();
});

const stubResponse = {
    reviewPoint: {
      repetitionCount: 5
    }
  }

describe('note statistics', () => {

  test('fetchBlogPosts API to be called ONCE with article rendered', async () => {
    fetch.mockResponseOnce(JSON.stringify({}));
    const wrapper = mount(NoteStatistics, {propsData: {noteid: 123}});
    await flushPromises()
    expect(fetch).toHaveBeenCalledTimes(1);
    expect(fetch).toHaveBeenCalledWith('/api/notes/123/statistics');
    expect(wrapper.findAll(".statistics-value")).toHaveLength(0)
  });

  test('should render values', async () => {
    fetch.mockResponseOnce(JSON.stringify(stubResponse));
    const wrapper = mount(NoteStatistics, {propsData: {noteid: 123}});
    await flushPromises()

    expect(wrapper.findAll(".statistics-value")).toHaveLength(3)
  });

});
