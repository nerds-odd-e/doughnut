import NoteShowPage from '@/pages/NoteShowPage.vue';
import { mount } from '@vue/test-utils';
import flushPromises from 'flush-promises';
import {noteViewedByUser} from '../notes/fixtures'
import _ from 'lodash'
import { createTestRouter } from '../testing_routes'

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
    const testingRouter = await createTestRouter();
    fetch.mockResponseOnce(JSON.stringify(stubResponse));
    const wrapper = mount(NoteShowPage, {propsData: {noteId: 123}, global: { plugins: [testingRouter] }});
    await flushPromises()
    expect(fetch).toHaveBeenCalledTimes(1);
    expect(fetch).toHaveBeenCalledWith('/api/notes/123', {});
    expect(wrapper.findAll(".nav a[title='Add to my learning']")).toHaveLength(0)
    expect(wrapper.findAll(".statistics-value")).toHaveLength(0)
  });
});
