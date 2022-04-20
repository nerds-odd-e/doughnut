/**
 * @jest-environment jsdom
 */
import { screen } from '@testing-library/vue';
import CommentCreateDialog from '@/components/notes/CommentCreateDialog.vue';
import helper from '../helpers';

describe('create a comment', () => {
  it('renders a dialog for comment with submit button', async () => {
    helper.component(CommentCreateDialog).render()
    await screen.findByText('Submit');
  });
});
