/**
 * @jest-environment jsdom
 */
import fetchMock from "jest-fetch-mock";
import CircleShowPage from '@/pages/CircleShowPage.vue';
import helper from '../helpers';
import makeMe from '../fixtures/makeMe';

beforeEach(() => {
  fetchMock.resetMocks();
  helper.reset()
});

describe('circle show page', () => {
  it('fetch API to be called ONCE on mount', async () => {
    const notebook = makeMe.aNotebook.please();
    const circleNote = makeMe.aCircleNote.notebooks(notebook).please();
    fetchMock.mockResponseOnce(JSON.stringify(circleNote));

    helper.component(CircleShowPage).withProps({ circleId: circleNote.id }).render()

    expect(fetchMock).toHaveBeenCalledTimes(1);
    expect(fetchMock).toHaveBeenCalledWith(
      `/api/circles/${circleNote.id}`,
      expect.anything()
    );
  });

  it('fetch API to be called every 5 seconds', async () => {
    const notebook = makeMe.aNotebook.please();
    const circleNote = makeMe.aCircleNote.notebooks(notebook).please();
    fetchMock.mockResponse(JSON.stringify(circleNote));

    helper.component(CircleShowPage).withProps({ circleId: circleNote.id }).render()

    expect(fetchMock).toHaveBeenCalledTimes(1);
    expect(fetchMock).toHaveBeenCalledWith(
      `/api/circles/${circleNote.id}`,
      expect.anything()
    );
  });
});
