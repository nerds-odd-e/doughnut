/**
 * @jest-environment jsdom
 */
import fetchMock from "jest-fetch-mock";
import BazaarPage from '@/pages/BazaarPage.vue';
import { StoredComponentTestHelper } from '../helpers';
import makeMe from '../fixtures/makeMe';

jest.useFakeTimers();

let helper: StoredComponentTestHelper

beforeEach(() => {
  fetchMock.resetMocks();
  helper = new StoredComponentTestHelper()
});

describe('bazaar page', () => {
  it('fetch API to be called ONCE on mount', async () => {
    const notebook = makeMe.aNotebook.please();
    const bazaarNotebooks = makeMe.bazaarNotebooks.notebooks(notebook).please();
    fetchMock.mockResponseOnce(JSON.stringify(bazaarNotebooks));

    helper.component(BazaarPage).render()

    expect(fetch).toHaveBeenCalledTimes(1);
    expect(fetch).toHaveBeenCalledWith('/api/bazaar', expect.anything());
  });
});
