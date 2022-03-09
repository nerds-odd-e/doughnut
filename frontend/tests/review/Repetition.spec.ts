/**
 * @jest-environment jsdom
 */
import fetchMock from "jest-fetch-mock";
import Repetition from '@/components/review/Repetition.vue';
import { StoredComponentTestHelper } from '../helpers';
import makeMe from '../fixtures/makeMe';

describe('repetition page', () => {
  let helper: StoredComponentTestHelper

  beforeEach(async () => {
    helper = new StoredComponentTestHelper()
    fetchMock.resetMocks();
    fetchMock.mockResponseOnce(JSON.stringify({}));
  });

  describe('repetition page for a note', () => {
    const note = makeMe.aNote.please();
    const reviewPointForView = makeMe.aReviewPoint.ofNote(note).please();

    it('for note', async () => {
      const wrapper = helper.component(Repetition).withProps(reviewPointForView).currentRoute({ name: 'root' }).mount()
      expect(wrapper.findAll('.btn-toolbar')).toHaveLength(1);
    });
  });

  describe('repetition page for a link', () => {
    const linkViewedByUser = makeMe.aLinkViewedByUser.please();
    const reviewPointForView = makeMe.aReviewPoint
      .ofLink(linkViewedByUser)
      .please();

    it('for link', async () => {
      const wrapper = helper.component(Repetition).withProps(reviewPointForView).currentRoute({ name: 'root' }).mount()
      expect(wrapper.findAll('.btn-toolbar')).toHaveLength(1);
    });

    it('click on note when doing review', async () => {
      const wrapper = helper.component(Repetition).withProps(reviewPointForView).currentRoute({ name: 'repeat' }).mount()
      expect(
        JSON.parse(wrapper.find('.link-source .router-link').attributes().to)
          .name
      ).toEqual('notebooks');
    });

    it('click on note when doing review and in a nested page', async () => {
      const wrapper = helper.component(Repetition).withProps(reviewPointForView).currentRoute({ name: 'repeat-noteShow', params: { noteId: 123 } }).mount()
      expect(
        JSON.parse(wrapper.find('.link-source .router-link').attributes().to)
      ).toEqual({ name: 'notebooks' });
    });
  });
});
