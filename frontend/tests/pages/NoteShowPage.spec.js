import NoteShowPage from '@/pages/NoteShowPage.vue';
import { mountWithMockRoute } from '../helpers'
import flushPromises from 'flush-promises';
import {noteViewedByUser} from '../notes/fixtures'
import _ from 'lodash'

beforeEach(() => {
  fetch.resetMocks();
});

const stubResponse = _.merge(noteViewedByUser,
    {
        notebook: {
            ownership: {
                isFromCircle: true,
                circle: {
                    name: "a circle"
                }
            },
        },
    }
  )

describe('note show', () => {

  test('fetch API to be called ONCE', async () => {
    fetch.mockResponseOnce(JSON.stringify(stubResponse));
    const { wrapper } = mountWithMockRoute(NoteShowPage, {propsData: {noteId: 123}});
    await flushPromises()
    expect(fetch).toHaveBeenCalledTimes(1);
    expect(fetch).toHaveBeenCalledWith('/api/notes/123', {});
    expect(wrapper.findAll(".nav a[title='Add to my learning']")).toHaveLength(0)
    expect(wrapper.findAll(".statistics-value")).toHaveLength(0)
  });
});
