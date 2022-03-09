/**
 * @jest-environment jsdom
 */
import { screen } from '@testing-library/vue';
import NoteCardsView from '@/components/notes/views/NoteCardsView.vue';
import { StoredComponentTestHelper } from '../helpers';
import makeMe from '../fixtures/makeMe';

describe('note wth child cards', () => {
  it('should render note with one child', async () => {
    const helper = new StoredComponentTestHelper()
    const notePosition = makeMe.aNotePosition.please();
    const noteParent = makeMe.aNote.title('parent').please();
    const noteChild = makeMe.aNote.title('child').under(noteParent).please();
    helper.store.loadNotes([noteParent, noteChild]);
    helper.component(NoteCardsView).withProps({ noteId: noteParent.id, notePosition, expandChildren: true }).render()
    expect(screen.getAllByRole('title')).toHaveLength(1);
    await screen.findByText('parent');
    await screen.findByText('child');
  });
});
