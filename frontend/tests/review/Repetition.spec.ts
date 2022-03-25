/**
 * @jest-environment jsdom
 */
import Repetition from '@/components/review/Repetition.vue';
import helper from '../helpers';
import makeMe from '../fixtures/makeMe';

describe('repetition page', () => {

  beforeEach(async () => {
    helper.reset()
  });

  describe('repetition page for a link', () => {
    const linkViewedByUser = makeMe.aLinkViewedByUser.please();
    const reviewPointViewedByUser = makeMe.aReviewPoint
      .ofLink(linkViewedByUser)
      .please();

    it('click on note when doing review', async () => {
      const wrapper = helper.component(Repetition).withProps({ reviewPointViewedByUser }).currentRoute({ name: 'repeat' }).mount()
      expect(
        JSON.parse(wrapper.find('.link-source .router-link').attributes().to)
          .name
      ).toEqual('notebooks');
    });

    it('click on note when doing review and in a nested page', async () => {
      const wrapper = helper.component(Repetition).withProps({ reviewPointViewedByUser }).currentRoute({ name: 'repeat-noteShow', params: { rawNoteId: 123 } }).mount()
      expect(
        JSON.parse(wrapper.find('.link-source .router-link').attributes().to)
      ).toEqual({ name: 'notebooks' });
    });
  });
});
