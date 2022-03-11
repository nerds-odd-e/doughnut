/**
 * @jest-environment jsdom
 */
import RepeatPage from '@/pages/RepeatPage.vue';
import flushPromises from 'flush-promises';
import helper from '../helpers';
import makeMe from '../fixtures/makeMe';
import RenderingHelper from "../helpers/RenderingHelper";

let renderer: RenderingHelper
let mockRouterPush = jest.fn();

helper.resetWithApiMock(beforeEach, afterEach)

beforeEach(() => {
  mockRouterPush = jest.fn();
  renderer = helper.component(RepeatPage).withMockRouterPush(mockRouterPush);
});

describe('repeat page', () => {
  let note = makeMe.aNote.please();
  const popupMock = { alert: jest.fn() };

  const mountPage = async (repetition: Generated.RepetitionForUser | {}) => {
    helper.store.loadNotes([note]);
    helper.apiMock.expecting('/api/reviews/repeat', repetition);
    const wrapper = renderer.withGlobalMock( {
      $popups: popupMock
    }).currentRoute({ name: 'repeat' }).mount()
    await flushPromises();
    return wrapper;
  };

  it('redirect to review page if nothing to repeat', async () => {
    await mountPage({});
    helper.apiMock.verifyCall('/api/reviews/repeat')
    expect(mockRouterPush).toHaveBeenCalledWith({ name: 'reviews' });
  });

  it('replace route with repeat/quiz if there is a quiz', async () => {
    const repetition = makeMe.aRepetition.ofNote(note).withAQuiz().please();
    await mountPage(repetition);
    expect(mockRouterPush).toHaveBeenCalledWith({ name: 'repeat-quiz' });
  });

  describe('repeat page with no quiz (or after quiz)', () => {
    let repetition: Generated.RepetitionForUser;

    beforeEach(()=>{
      note = makeMe.aNote.please();
      helper.store.loadNotes([note]);
      repetition = makeMe.aRepetition.ofNote(note).please();
    })

    it('stay at repeat page if there is no quiz', async () => {
      await mountPage(repetition);
      expect(mockRouterPush).toHaveBeenCalledWith({
        name: 'repeat',
        replace: true,
      });
    });

    it('should call the self-evaluate api', async () => {
      repetition = makeMe.aRepetition.ofNote(note).please();
      const reviewPointId = repetition.reviewPointViewedByUser.reviewPoint.id;
      const wrapper = await mountPage(repetition);
      helper.apiMock.expecting(`/api/reviews/${reviewPointId}/self-evaluate`)
      await wrapper.find('#repeat-sad').trigger('click');
    });

    it('reload next review point if 404', async () => {
      repetition = makeMe.aRepetition.ofNote(note).please();
      const reviewPointId = repetition.reviewPointViewedByUser.reviewPoint.id;
      const wrapper = await mountPage(repetition);

      helper.apiMock.expectingResponse(`/api/reviews/${reviewPointId}/self-evaluate`, {status: 404})

      wrapper.find('#repeat-sad').trigger('click');
      await flushPromises();
      expect(popupMock.alert).toHaveBeenCalledWith(
        expect.stringMatching(/review point/)
      );
      helper.apiMock.verifyCall(`/api/reviews/repeat`)
    });
  });
});
