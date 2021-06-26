import NoteViewedByUser from '@/components/notes/NoteViewedByUser.vue';
import { mount } from '@vue/test-utils';
import {noteViewedByUser} from './fixtures'
import _ from 'lodash'
import { createTestRouter } from '../testing_routes'

const noteView = _.merge(noteViewedByUser,
    {
        owns: false,
    }
  )

describe('a note not owned by viewer', () => {
  var testingRouter;

  beforeEach(async () => {
    testingRouter = await createTestRouter();
    fetch.resetMocks();
    testingRouter.push('/')
    await testingRouter.isReady()
  });


  test('', async () => {
    const wrapper = mount(NoteViewedByUser, {propsData: noteView, global: { plugins: [testingRouter] }})
    expect(wrapper.find(".breadcrumb").text()).toContain("Bazaar")
    expect(wrapper.findAll(".nav a[title='Add to my learning']")).toHaveLength(1)
  });
});
