import Repetition from '@/components/review/Repetition.vue';
import { mount } from '@vue/test-utils';
import { createTestRouter } from '../testing_routes'
import { noteViewedByUser, linkViewedByUser } from "../notes/fixtures"

describe('repetition page', () => {
  beforeEach(async () => {
    fetch.resetMocks();
    fetch.mockResponseOnce(JSON.stringify({}));
  });

  test('for note', async () => {
    const testingRouter = await createTestRouter("/");
    const reviewPointForView = {
      reviewPoint: {
        id: 3,
      },
      noteViewedByUser: noteViewedByUser,
    }
    const wrapper = mount(Repetition, {propsData: reviewPointForView, global: { plugins: [testingRouter] }});
    expect(wrapper.findAll(".btn-toolbar")).toHaveLength(1)
  });

  describe('repetition page for a link', () => {
    const reviewPointForView = {
      reviewPoint: {
        id: 3,
      },
      linkViewedByUser: linkViewedByUser,
    }

    test('for link', async () => {
      const testingRouter = await createTestRouter("/");
      const wrapper = mount(Repetition, {propsData: reviewPointForView, global: { plugins: [testingRouter] }});
      expect(wrapper.findAll(".btn-toolbar")).toHaveLength(1)
    });

    xtest('click on note when doing review', async () => {
      const testingRouter = await createTestRouter({name: 'repeat'});
      const wrapper = mount(Repetition, {propsData: reviewPointForView, global: { plugins: [testingRouter] }});
      expect(wrapper.find(".link-source .card-title a").attributes().href).toEqual("/reviews/repeat/notes/2")
    });

    xtest('click on note when doing review and in a nested page', async () => {
      const testingRouter = await createTestRouter({name: 'repeat-noteShow', params: {noteid: 123}});
      const wrapper = mount(Repetition, {propsData: reviewPointForView, global: { plugins: [testingRouter] }});
      expect(wrapper.find(".link-source .card-title a").attributes().href).toEqual("/reviews/repeat/notes/2")
    });

  });

});
