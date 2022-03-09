/**
 * @jest-environment jsdom
 */
import fetchMock from "jest-fetch-mock";
import NoteEditDialog from '@/components/notes/NoteEditDialog.vue';
import flushPromises from 'flush-promises';
import _ from 'lodash';
import { StoredComponentTestHelper } from '../helpers';
import makeMe from '../fixtures/makeMe';

let helper: StoredComponentTestHelper

beforeEach(() => {
  fetchMock.resetMocks();
  helper = new StoredComponentTestHelper()
});

describe('note show', () => {
  it('fetch API to be called ONCE', async () => {
    const note = makeMe.aNote.please();
    const stubResponse = {
      notePosition: makeMe.aNotePosition.please(),
      notes: [note],
    };
    fetchMock.mockResponseOnce(JSON.stringify(stubResponse));
    helper.component(NoteEditDialog).withProps({ noteId: note.id }).render()
    await flushPromises();
    expect(fetchMock).toHaveBeenCalledTimes(1);
    expect(fetchMock).toHaveBeenCalledWith(
      `/api/notes/${note.id}`,
      expect.anything()
    );
  });
});
