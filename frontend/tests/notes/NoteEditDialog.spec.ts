/**
 * @jest-environment jsdom
 */
import NoteEditDialog from '@/components/notes/NoteEditDialog.vue';
import helper from '../helpers';
import makeMe from '../fixtures/makeMe';

helper.resetWithApiMock(beforeEach, afterEach)

describe('note show', () => {
  it('fetch API to be called ONCE', async () => {
    const note = makeMe.aNote.please();
    const stubResponse = {
      notePosition: makeMe.aNotePosition.please(),
      notes: [note],
    };

    helper.apiMock.expecting(`/api/notes/${note.id}`, stubResponse);
    helper.component(NoteEditDialog).withProps({ noteId: note.id }).render()
  });
});
