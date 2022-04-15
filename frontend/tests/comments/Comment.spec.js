/**
 * @jest-environment jsdom
 */
import NoteCardsView from '@/components/notes/views/NoteCardsView.vue';
import helper from '../helpers';
import makeMe from "../fixtures/makeMe";

helper.resetWithApiMock(beforeEach, afterEach)

describe('delete comment', () => {
  it('should be call once', async () => {
    const noteRealm = makeMe.aNoteRealm
    .withCommentOfId(237)
    .withCommentOfId(567).please();
    helper.store.setFeatureToggle(true);
    helper.store.loadNoteRealms([noteRealm]);
    const wrapper = helper.component(NoteCardsView).withProps(
      {noteId: noteRealm.id, expandChildren: true}).mount()
    await wrapper.find('#comment-237-delete').trigger('click');
    helper.apiMock.verifyCall(`/api/notes/${noteRealm.id}/comments/delete/237`);
  });
});
