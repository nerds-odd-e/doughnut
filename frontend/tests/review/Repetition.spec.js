import Repetition from '@/components/review/Repetition.vue';
import { mount } from '@vue/test-utils';
import { router } from '@/routes'
import { noteViewedByUser, linkViewedByUser } from "../notes/fixtures"

beforeEach(async () => {
  fetch.resetMocks();
  router.push('/')
  await router.isReady()
});

describe('repetition page', () => {

  test('for note', async () => {
    const reviewPointForView = {
      reviewPoint: {
        id: 3,
      },
      noteViewedByUser: noteViewedByUser,
    }
    const wrapper = mount(Repetition, {propsData: reviewPointForView, global: { plugins: [router] }});
    expect(wrapper.findAll(".btn-toolbar")).toHaveLength(1)
  });

  xtest('for link', async () => {
    const reviewPointForView = {
      reviewPoint: {
        id: 3,
      },
      linkViewedByUser: linkViewedByUser,
    }
    const wrapper = mount(Repetition, {propsData: reviewPointForView, global: { plugins: [router] }});
    expect(wrapper.findAll(".btn-toolbar")).toHaveLength(1)
  });

});
