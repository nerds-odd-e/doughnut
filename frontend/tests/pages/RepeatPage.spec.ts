/**
 * @jest-environment jsdom
 */
import fetchMock from "jest-fetch-mock";
import RepeatPage from '@/pages/RepeatPage.vue';
import flushPromises from 'flush-promises';
import { StoredComponentTestHelper } from '../helpers';
import makeMe from '../fixtures/makeMe';
import RenderingHelper from "../helpers/RenderingHelper";

let helper: StoredComponentTestHelper
let renderer: RenderingHelper
let mockRouterPush = jest.fn();

beforeEach(() => {
  fetchMock.resetMocks();
  mockRouterPush = jest.fn();
  helper = new StoredComponentTestHelper()
  renderer = helper.component(RepeatPage).withMockRouterPush(mockRouterPush);
});

describe('repeat page', () => {
  let note = makeMe.aNote.please();
  const popupMock = { alert: jest.fn() };

  const mountPage = async (repetition: any) => {
    helper.store.loadNotes([note]);
    fetchMock.mockResponseOnce(JSON.stringify(repetition));
    const wrapper = renderer.withGlobal(
        {
          mocks: {
            $popups: popupMock,
          },
        }).currentRoute({ name: 'repeat' }).mount()
    await flushPromises();
    return wrapper;
  };

  it('redirect to review page if nothing to repeat', async () => {
    await mountPage({});
    expect(fetchMock).toHaveBeenCalledTimes(1);
    expect(fetchMock).toHaveBeenCalledWith(
      '/api/reviews/repeat',
      expect.anything()
    );
    expect(mockRouterPush).toHaveBeenCalledWith({ name: 'reviews' });
  });

  it('replace route with repeat/quiz if there is a quiz', async () => {
    const repetition = makeMe.aRepetition.ofNote(note).withAQuiz().please();
    await mountPage(repetition);
    expect(mockRouterPush).toHaveBeenCalledWith({ name: 'repeat-quiz' });
  });

  describe('repeat page with no quiz (or after quiz)', () => {
    let repetition: any;

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
      const wrapper = await mountPage(repetition);
      fetchMock.mockResponseOnce(JSON.stringify({}));
      wrapper.find('#repeat-sad').trigger('click');
      const reviewPointId = repetition.reviewPointViewedByUser.reviewPoint.id;
      expect(fetchMock).toHaveBeenCalledWith(
        `/api/reviews/${reviewPointId}/self-evaluate`,
        expect.anything()
      );
    });

    it('reload next review point if 404', async () => {
      repetition = makeMe.aRepetition.ofNote(note).please();
      const wrapper = await mountPage(repetition);

      fetchMock.mockClear();
      fetchMock.mockResponseOnce('', {status: 404});
      fetchMock.mockResponseOnce(JSON.stringify({}));

      wrapper.find('#repeat-sad').trigger('click');
      await flushPromises();
      expect(popupMock.alert).toHaveBeenCalledWith(
        expect.stringMatching(/review point/)
      );
      expect(fetchMock).toHaveBeenCalledWith(
        '/api/reviews/repeat',
        expect.anything()
      );
    });
  });
});
