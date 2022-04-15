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
    noteRealm.note.comments = [{
      id: 1,
      author: {
        id: 0,
        name: "old_learner",
        externalIdentifier: "",
        ownership: {
          id: 0,
        },
        dailyNewNotesCount: 0,
        spaceIntervals: "",
      },
      description: "hello world",
      createdAt: "yyyy-MM-dd",
    }]
    helper.store.setFeatureToggle(true);
    helper.store.loadNoteRealms([noteRealm]);
    const wrapper = helper.component(NoteCardsView).withProps(
      {noteId: noteRealm.id, expandChildren: true}).mount()
    await wrapper.find('#comment-1-delete').trigger('click');
  });
});
