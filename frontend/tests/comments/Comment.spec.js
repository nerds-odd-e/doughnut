/**
 * @jest-environment jsdom
 */
import helper from '../helpers';

helper.resetWithApiMock(beforeEach, afterEach)

describe('delete comment', () => {
  it('should be call once', async () => {
    // const noteRealm = makeMe.aNoteRealm.please();
    // const stubResponse = {
    //   notePosition: makeMe.aNotePosition.please(),
    //   notes: [noteRealm],
    // };
    //
    // helper.apiMock.expecting(`/api/notes/${noteRealm.id}`, stubResponse);
    // helper.component(NoteCardsView).withProps({noteId: noteRealm.id}).render()
  });
});
