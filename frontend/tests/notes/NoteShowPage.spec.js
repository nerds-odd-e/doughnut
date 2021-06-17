import NoteShowPage from '@/components/notes/NoteShowPage.vue';
import { mount } from '@vue/test-utils';
import flushPromises from 'flush-promises';
import {noteViewedByUser} from './fixtures'
import _ from 'lodash'
import { router } from '@/routes'

beforeEach(() => {
  fetch.resetMocks();
});

const stubResponse = _.merge(noteViewedByUser,
    {
        ownership: {
            isFromCircle: true,
            circle: {
                name: "a circle"
            }
        },
    }
  )

describe('note show', () => {

  test('fetch API to be called ONCE', async () => {
    fetch.mockResponseOnce(JSON.stringify(stubResponse));
    const wrapper = mount(NoteShowPage, {propsData: {noteid: 123}, global: { plugins: [router] }});
    await flushPromises()
    expect(fetch).toHaveBeenCalledTimes(1);
    expect(fetch).toHaveBeenCalledWith('/api/notes/123');
    expect(wrapper.findAll(".nav a[title='Add to my learning']")).toHaveLength(0)
    expect(wrapper.findAll(".statistics-value")).toHaveLength(0)
  });
});
