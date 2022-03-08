/**
 * @jest-environment jsdom
 */
import LinkNoteDialog from '@/components/links/LinkNoteDialog.vue';
import { screen } from '@testing-library/vue';
import { mount } from '@vue/test-utils';

describe('LinkNoteDialog', () => {
  it('Search at the top level with no note', async () => {
    mount(LinkNoteDialog, {
      propsData: {
        note: null,
      },
    });
    screen.findByTitle('Searching1');
  });
});
