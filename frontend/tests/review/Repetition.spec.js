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

  describe('repetition page for a note', () => {
    const reviewPointForView = {
      reviewPoint: {
        id: 3,
      },
      noteViewedByUser: noteViewedByUser,
    }

    test('for note', async () => {
      const testingRouter = await createTestRouter("/");
      const wrapper = mount(Repetition, {propsData: reviewPointForView, global: { plugins: [testingRouter] }});
      expect(wrapper.findAll(".btn-toolbar")).toHaveLength(1)
    });

    test('click pause with show the note', async () => {
      const { wrapper } = mountWithMockRoute(Repetition, {propsData: reviewPointForView}, {name: 'repeat'});
      expect(JSON.parse(wrapper.find(".pause-repeat").attributes().to)).toEqual({name: 'noteShow', params: {noteId: 2}})
    });

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

    test('click pause will go to nested page of showing the link', async () => {
      const { wrapper } = mountWithMockRoute(Repetition, {propsData: reviewPointForView}, {name: 'repeat-noteShow', params: {noteId: 123}});
      expect(JSON.parse(wrapper.find(".pause-repeat").attributes().to)).toEqual({name: 'linkShow', params: {linkid: 8}})
    });

    test('click on note when doing review', async () => {
      const { wrapper } = mountWithMockRoute(Repetition, {propsData: reviewPointForView}, {name: 'repeat'});
      expect(JSON.parse(wrapper.find(".link-source .router-link").attributes().to).name).toEqual("notebooks")
    });

    test('click on note when doing review and in a nested page', async () => {
      const { wrapper } = mountWithMockRoute(Repetition, {propsData: reviewPointForView}, {name: 'repeat-noteShow', params: {noteId: 123}});
      expect(JSON.parse(wrapper.find(".link-source .router-link").attributes().to)).toEqual({name: 'notebooks'})
    });

    test('compact view', async () => {
      const { wrapper, mockRouter } = mountWithMockRoute(Repetition, {propsData: {...reviewPointForView, compact: true}}, {name: 'repeat-noteShow', params: {noteId: 123}});
      expect(wrapper.find(".link-source .router-link").exists()).toBe(false)
      await wrapper.find(".repeat-container").trigger('click')
      expect(mockRouter.push).toHaveBeenCalledWith({name: 'repeat'})
    });

  });

});
