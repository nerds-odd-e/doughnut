/**
 * @jest-environment jsdom
 */
import helper from '../helpers';
import NoteCardsView from '@/components/notes/views/NoteCardsView.vue';
import makeMe from "../fixtures/makeMe";

helper.resetWithApiMock(beforeEach, afterEach)

describe('delete comment', () => {
  it('should be call once', async () => {
    const noteRealm = makeMe.aNoteRealm.please();
    const wrapper = helper.component(NoteCardsView).withProps(
      {noteId: noteRealm.id}).mount()
    // await wrapper.find('comment-1-delete').trigger('click');
  });
});
