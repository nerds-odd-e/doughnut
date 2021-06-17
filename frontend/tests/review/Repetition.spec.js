import Repetition from '@/components/review/Repetition.vue';
import { mount } from '@vue/test-utils';
import { noteViewedByUser } from "../notes/fixtures"

const reviewPointForView = {
  reviewPoint: {
    id: 3,
  },
  sourceNoteViewedByUser: noteViewedByUser,
}
describe('repetition page', () => {

  test('', async () => {
    const wrapper = mount(Repetition, {propsData: {...reviewPointForView}});
    expect(wrapper.findAll(".btn-toolbar")).toHaveLength(1)
  });
});
