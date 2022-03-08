/**
 * @jest-environment jsdom
 */
import { screen } from '@testing-library/vue';
import NotebooksPage from '@/pages/NotebooksPage.vue';
import store from '../fixtures/testingStore';
import { renderWithStoreAndMockRoute } from '../helpers';
import _ from 'lodash';
import makeMe from '../fixtures/makeMe';

beforeEach(() => {
  fetch.resetMocks();
  fetch.mockResponse(
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

    fetch.mockResponse(JSON.stringify(stubResponse));
    renderWithStoreAndMockRoute(store, NotebooksPage, {});

    expect(fetch).toHaveBeenCalledTimes(1);
    expect(fetch).toHaveBeenCalledWith('/api/notebooks', expect.anything());
    expect(await screen.findByTitle('undo')).toBeDisabled();
  });

  it('show undo when there is something to undo', async () => {
    const notebook = makeMe.aNotebook.please();
    store.loadNotes([notebook.headNote]);
    store.deleteNote(notebook.headNote.id);

    renderWithStoreAndMockRoute(store, NotebooksPage, {});

    expect(await screen.findByTitle('undo delete note')).not.toBeDisabled();
  });
});
