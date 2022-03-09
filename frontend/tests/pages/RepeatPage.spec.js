/**
 * @jest-environment jsdom
 */
import RepeatPage from '@/pages/RepeatPage.vue';
import flushPromises from 'flush-promises';
import _ from 'lodash';
import store from '../fixtures/testingStore';
import { mountWithStoreAndMockRoute } from '../helpers';
import makeMe from '../fixtures/makeMe';

beforeEach(() => {
  fetch.resetMocks();
});

describe('repeat page', () => {
  let note = makeMe.aNote.please();
  const popupMock = { alert: jest.fn() };

  const mountPage = async (repetition) => {
    store.loadNotes([note]);
    fetch.mockResponseOnce(JSON.stringify(repetition));
    const { wrapper, mockRouter } = mountWithStoreAndMockRoute(
      store,
      RepeatPage,
      {
        global: {
          mocks: {
            $popups: popupMock,
          },
        },
      },
      { name: 'repeat' }
    );
    await flushPromises();
    return { wrapper, mockRouter };
  };

  it('redirect to review page if nothing to repeat', async () => {
    const { mockRouter } = await mountPage({});
    expect(fetch).toHaveBeenCalledTimes(1);
    expect(fetch).toHaveBeenCalledWith(
      '/api/reviews/repeat',
      expect.anything()
    );
    expect(mockRouter.push).toHaveBeenCalledWith({ name: 'reviews' });
  });

  it('replace route with repeat/quiz if there is a quiz', async () => {
    const repetition = makeMe.aRepetition.ofNote(note).withAQuiz().please();
    const { mockRouter } = await mountPage(repetition);
    expect(mockRouter.push).toHaveBeenCalledWith({ name: 'repeat-quiz' });
  });

  describe('repeat page with no quiz (or after quiz)', () => {
    note = makeMe.aNote.please();
    store.loadNotes([note]);
    let repetition = makeMe.aRepetition.ofNote(note).please();

    it('stay at repeat page if there is no quiz', async () => {
      const { mockRouter } = await mountPage(repetition);
      expect(mockRouter.push).toHaveBeenCalledWith({
        name: 'repeat',
        replace: true,
      });
    });

    it('should call the self-evaluate api', async () => {
      repetition = makeMe.aRepetition.ofNote(note).please();
      const { wrapper } = await mountPage(repetition);
      fetch.mockResponseOnce(JSON.stringify({}));
      wrapper.find('#repeat-sad').trigger('click');
      const reviewPointId = repetition.reviewPointViewedByUser.reviewPoint.id;
      expect(fetch).toHaveBeenCalledWith(
        `/api/reviews/${reviewPointId}/self-evaluate`,
        expect.anything()
      );
    });

    it('reload next review point if 404', async () => {
      repetition = makeMe.aRepetition.ofNote(note).please();
      const { wrapper } = await mountPage(repetition);

      fetch.mockClear();
      fetch.mockRejectOnce({ status: 404 });
      fetch.mockResponseOnce(JSON.stringify({}));

      wrapper.find('#repeat-sad').trigger('click');
      await flushPromises();
      expect(popupMock.alert).toHaveBeenCalledWith(
        expect.stringMatching(/review point/)
      );
      expect(fetch).toHaveBeenCalledWith(
        '/api/reviews/repeat',
        expect.anything()
      );
    });
  });
});
