import Repetition from '@/components/review/Repetition.vue';
import { mount } from '@vue/test-utils';
import { noteViewedByUser, linkViewedByUser } from "../notes/fixtures"

describe('repetition page', () => {

  test('for note', async () => {
    const reviewPointForView = {
      reviewPoint: {
        id: 3,
      },
      noteViewedByUser: noteViewedByUser,
    }
    const wrapper = mount(Repetition, {propsData: {...reviewPointForView}});
    expect(wrapper.findAll(".btn-toolbar")).toHaveLength(1)
  });

  xtest('for link', async () => {
    const reviewPointForView = {
      reviewPoint: {
        id: 3,
      },
      linkViewedByUser: linkViewedByUser,
    }
    const wrapper = mount(Repetition, {propsData: {...reviewPointForView}});
    expect(wrapper.findAll(".btn-toolbar")).toHaveLength(1)
  });

});
