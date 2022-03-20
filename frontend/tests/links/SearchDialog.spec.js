/**
 * @jest-environment jsdom
 */
import LinkNoteDialog from '@/components/links/LinkNoteDialog.vue';
import { render, screen } from '@testing-library/vue';

describe('LinkNoteDialog', () => {
  it('Search at the top level with no note', async () => {
    render(LinkNoteDialog, {
      propsData: {
        note: null,
      },
    });
    await screen.findByText('Searching');
    expect(await screen.findByLabelText("All My Notebooks And Subscriptions")).toBeDisabled();
  });
});
