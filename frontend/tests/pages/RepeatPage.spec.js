import RepeatPage from '@/pages/RepeatPage.vue';
import flushPromises from 'flush-promises';
import {noteViewedByUser} from '../notes/fixtures'
import _ from 'lodash'
import { mountWithMockRoute } from '../helpers'

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
    const wrapper = mountWithMockRoute(RepeatPage, {}, {name: 'repeat'});
    await flushPromises()
    expect(fetch).toHaveBeenCalledTimes(1);
    expect(fetch).toHaveBeenCalledWith('/api/reviews/repeat', {});
  });
});
