/**
 * @jest-environment jsdom
 */
import LinkNoteDialog from '@/components/links/LinkNoteDialog.vue';
import { render, screen } from '@testing-library/vue';
import { flushPromises } from '@vue/test-utils';
import MakeMe from '../fixtures/makeMe';

describe('LinkNoteDialog', () => {

  it('Search at the top level with no note', async () => {
    render(LinkNoteDialog, { propsData: { note: null } });
    await screen.findByText('Searching');
    expect(await screen.findByLabelText("All My Notebooks And Subscriptions")).toBeDisabled();
  });

  it('Search from a note', async () => {
    const note = MakeMe.aNote.please()
    render(LinkNoteDialog, { propsData: { note } });
    await screen.findByText(`Link to`);
  });

  it('toggle search settings', async () => {
    const note = MakeMe.aNote.please()
    render(LinkNoteDialog, { propsData: { note } });
    (await screen.findByLabelText("All My Circles")).click()
    expect(await screen.findByLabelText("All My Notebooks And Subscriptions")).toBeChecked()
    flushPromises();
    (await screen.findByLabelText("All My Notebooks And Subscriptions")).click()
    expect(await screen.findByLabelText("All My Circles")).not.toBeChecked()
  });

});
