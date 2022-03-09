/**
 * @jest-environment jsdom
 */
import fetchMock from "jest-fetch-mock";
import { screen } from '@testing-library/vue';
import NotebooksPage from '@/pages/NotebooksPage.vue';
import helper from '../helpers';
import makeMe from '../fixtures/makeMe';

beforeEach(() => {
  helper.reset()
  fetchMock.resetMocks();
  fetchMock.mockResponse(
    JSON.stringify({
      notebooks: [],
      subscriptions: [],
    })
  );
});

describe('Notebooks Page', () => {
  it('fetch API to be called ONCE', async () => {
    const notebook = makeMe.aNotebook.please();
    const stubResponse = {
      notebooks: [notebook],
      subscriptions: [],
    };

    fetchMock.mockResponse(JSON.stringify(stubResponse));
    helper.component(NotebooksPage).render();

    expect(fetchMock).toHaveBeenCalledTimes(1);
    expect(fetchMock).toHaveBeenCalledWith('/api/notebooks', expect.anything());
    expect(await screen.findByTitle('undo')).toBeDisabled();
  });

  it('show undo when there is something to undo', async () => {
    const notebook = makeMe.aNotebook.please();
    helper.store.loadNotes([notebook.headNote]);
    helper.store.deleteNote(notebook.headNote.id);

    helper.component(NotebooksPage).render();

    expect(await screen.findByTitle('undo delete note')).not.toBeDisabled();
  });
});
