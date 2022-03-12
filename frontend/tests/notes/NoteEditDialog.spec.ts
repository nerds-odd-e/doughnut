/**
 * @jest-environment jsdom
 */
import NoteEditDialog from '@/components/notes/NoteEditDialog.vue';
import helper from '../helpers';
import makeMe from '../fixtures/makeMe';

helper.resetWithApiMock(beforeEach, afterEach)

describe('note show', () => {
  it('fetch API to be called ONCE', async () => {
    const noteSphere = makeMe.aNoteSphere.please();
    const stubResponse = {
      notePosition: makeMe.aNotePosition.please(),
      notes: [noteSphere],
    };

    helper.apiMock.expecting(`/api/notes/${noteSphere.id}`, stubResponse);
    helper.component(NoteEditDialog).withProps({ noteId: noteSphere.id }).render()
  });
});
