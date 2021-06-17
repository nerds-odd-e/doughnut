import NoteViewedByUser from '@/components/notes/NoteViewedByUser.vue';
import { mount } from '@vue/test-utils';
import flushPromises from 'flush-promises';
import {noteViewedByUser} from './fixtures'
import _ from 'lodash'
import { router } from '@/routes'

beforeEach(() => {
  fetch.resetMocks();
});

const noteView = _.merge(noteViewedByUser,
    {
        owns: false,
    }
  )

describe('a note not owned by viewer', () => {

  test('', async () => {
    const wrapper = mount(NoteViewedByUser, {propsData: noteView, global: { plugins: [router] }});
    expect(wrapper.find(".breadcrumb").text()).toContain("Bazxaar")
  });
});
