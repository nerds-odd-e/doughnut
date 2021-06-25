import NoteViewedByUser from '@/components/notes/NoteViewedByUser.vue';
import { mount } from '@vue/test-utils';
import {noteViewedByUser} from './fixtures'
import _ from 'lodash'
import { router } from '@/routes'

beforeEach(async () => {
  fetch.resetMocks();
  router.push('/')
  await router.isReady()
});

const noteView = _.merge(noteViewedByUser,
    {
        owns: false,
    }
  )

describe('a note not owned by viewer', () => {

  test('', async () => {
    const wrapper = mount(NoteViewedByUser, {propsData: noteView, global: { plugins: [router] }})
    expect(wrapper.find(".breadcrumb").text()).toContain("Bazaar")
    expect(wrapper.findAll(".nav a[title='Add to my learning']")).toHaveLength(1)
  });
});
