/**
 * @jest-environment jsdom
 */
import fetchMock from "jest-fetch-mock";
import { screen } from '@testing-library/vue';
import NoteShowPage from '@/pages/NoteShowPage.vue';
import NoteWithLinks from '@/components/notes/NoteWithLinks.vue';
import helper from '../helpers';
import makeMe from '../fixtures/makeMe';
import { viewType } from '../../src/models/viewTypes';

jest.useFakeTimers();

beforeEach(() => {
  fetchMock.resetMocks();
  helper.reset()
});

describe('all in note show page', () => {
  describe('note show', () => {
    const note = makeMe.aNote.please();
    const stubResponse = {
      notePosition: makeMe.aNotePosition.inCircle('a circle').please(),
      notes: [note],
    };

    beforeEach(() => {
      fetchMock.mockResponse(JSON.stringify(stubResponse));
    });

    it(' should fetch API to be called TWICE when viewType is not included ', async () => {
      helper.component(NoteShowPage).withProps({ noteId: note.id }).render();
      jest.advanceTimersByTime(5000);
      expect(fetchMock).toHaveBeenCalledTimes(1);
      expect(fetchMock).toHaveBeenCalledWith(
        `/api/notes/${note.id}`,
        expect.anything()
      );
      await screen.findByText('a circle');
    });

    it(' should fetch API to be called when viewType is mindmap ', async () => {
      const viewTypeValue = 'mindmap';
      helper.component(NoteShowPage).withProps({ noteId: note.id, viewType: viewTypeValue }).render()
      expect(viewType(viewTypeValue)?.fetchAll).toBe(true);
      expect(fetchMock).toHaveBeenCalledTimes(1);
      expect(fetchMock).toHaveBeenCalledWith(
        `/api/notes/${note.id}/overview`,
        expect.anything()
      );
      await screen.findByText('a circle');
    });
  });

  describe('polling data', () => {
    it('should not call fetch API when inputing text ', async () => {
      const note = makeMe.aNote.title('Dummy Title').please();
      helper.store.loadNotes([note]);

      const wrapper = helper.component(NoteWithLinks).withProps( { note }).mount()

      await wrapper.find('[role="title"]').trigger('click');
      await wrapper.find('[role="title"] input').trigger('input');

      jest.advanceTimersByTime(5000);
      expect(fetchMock).not.toHaveBeenCalledWith(`/api/notes/${note.id}`, {});
    });
  });
});
