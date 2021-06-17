import Repetition from '@/components/review/Repetition.vue';
import { mount } from '@vue/test-utils';

const reviewPoint = {
  id: 3,
}
describe('repetition page', () => {

  test('', async () => {
    const wrapper = mount(Repetition, {propsData: {reviewPoint}});
    expect(wrapper.findAll(".btn-toolbar")).toHaveLength(1)
  });
});
