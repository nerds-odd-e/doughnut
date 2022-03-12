/**
 * @jest-environment jsdom
 */
import InitialReviewPage from '@/pages/InitialReviewPage.vue';
import flushPromises from 'flush-promises';
import helper from '../helpers';
import makeMe from '../fixtures/makeMe';
import RenderingHelper from "../helpers/RenderingHelper";

let renderer: RenderingHelper
let mockRouterPush = jest.fn();

helper.resetWithApiMock(beforeEach, afterEach)

beforeEach(() => {
  mockRouterPush = jest.fn();
  renderer = helper.component(InitialReviewPage).withMockRouterPush(mockRouterPush);
});

describe('repeat page', () => {
  it('redirect to review page if nothing to review', async () => {
    renderer.currentRoute({ name: 'initial' }).mount()
    await flushPromises();
    helper.apiMock.verifyCall('/api/reviews/initial')
    expect(mockRouterPush).toHaveBeenCalledWith({ name: 'reviews' });
  });

  it('normal view', async () => {
    const note = makeMe.aNote.please();
    const reviewPoint = makeMe.aReviewPoint
      .ofNote(note)
      .remainingInitialReviewCountForToday(53)
      .please();
    helper.apiMock.expecting('/api/reviews/initial', reviewPoint)

    const wrapper = renderer.currentRoute({ name: 'initial' }).mount()
    await flushPromises();
    expect(mockRouterPush).toHaveBeenCalledTimes(0);
    expect(wrapper.findAll('.initial-review-container')).toHaveLength(0);
    expect(wrapper.findAll('.pause-stop')).toHaveLength(1);
    expect(wrapper.find('.progress-text').text()).toContain(
      'Initial Review: 0/53'
    );
  });

  it('minimized view', async () => {
    const noteSphere = makeMe.aNote.please();
    const reviewPoint = makeMe.aReviewPoint.ofNote(noteSphere).please();
    helper.apiMock.expecting('/api/reviews/initial', reviewPoint)
    const wrapper = renderer.withProps({nested: true}).currentRoute({ name: 'initial' }).mount()
    await flushPromises();
    expect(mockRouterPush).toHaveBeenCalledTimes(0);
    expect(wrapper.findAll('.initial-review-container')).toHaveLength(1);
    expect(wrapper.find('.review-point-abbr span').text()).toContain(
      noteSphere.title
    );
  });
});
