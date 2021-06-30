import Repetition from '@/components/review/Repetition.vue';
import { createTestRouter } from '../testing_routes'
import { noteViewedByUser, linkViewedByUser } from "../notes/fixtures"
import { mount } from '@vue/test-utils';
import { mountWithMockRoute } from '../helpers'

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

    test('click on note when doing review', async () => {
      const { wrapper } = mountWithMockRoute(Repetition, {propsData: reviewPointForView}, {name: 'repeat'});

      expect(JSON.parse(wrapper.find(".link-source .router-link").attributes().to).name).toEqual("repeat-noteShow")
    });

    test('click on note when doing review and in a nested page', async () => {
      const { wrapper } = mountWithMockRoute(Repetition, {propsData: reviewPointForView}, {name: 'repeat-noteShow', params: {noteid: 123}});
      expect(JSON.parse(wrapper.find(".link-source .router-link").attributes().to)).toEqual({name: 'repeat-noteShow', params: {noteid: 1}})
    });

    test('compact view', async () => {
      const { wrapper } = mountWithMockRoute(Repetition, {propsData: {...reviewPointForView, compact: true}}, {name: 'repeat-noteShow', params: {noteid: 123}});
      expect(wrapper.find(".back-to-repeat").exists()).toBe(true)
      expect(wrapper.find(".link-source .router-link").exists()).toBe(false)
    });

  });

});
