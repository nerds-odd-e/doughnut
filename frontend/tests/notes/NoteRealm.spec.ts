/**
 * @jest-environment jsdom
 */

import {screen} from '@testing-library/vue';
import NoteRealm from "@/components/notes/views/NoteRealm.vue";
import makeMe from '../fixtures/makeMe';
import helper from '../helpers';

describe('note realm overview', () => {

  beforeEach(() => {
    helper.reset();
    helper.store.setFeatureToggle(true)
  });

  it('should render reply-input', () => {
    const note = makeMe.aNoteRealm.title('single note').please();
    helper.store.loadNoteRealms([note]);
    helper.component(NoteRealm).withProps({
      noteId: note.id,
      expandChildren: false
    }).render();
    expect(screen.getByTestId('reply-input')).toHaveAttribute('placeholder', 'Reply...');
  });
});
