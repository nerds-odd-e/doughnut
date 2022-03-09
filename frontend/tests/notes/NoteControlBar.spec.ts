/**
 * @jest-environment jsdom
 */
import { screen } from '@testing-library/vue';
import Breadcrumb from '@/components/notes/Breadcrumb.vue';
import { StoredComponentTestHelper } from '../helpers';
import makeMe from '../fixtures/makeMe';

describe('note wth child cards', () => {
  let helper: StoredComponentTestHelper

  beforeEach(()=>{
    helper = new StoredComponentTestHelper()
  })

  it('view note belongs to other people in bazaar', async () => {
    const note = makeMe.aNote.please();
    const notePosition = makeMe.aNotePosition.inBazaar().please();
    helper.store.loadNotes([note]);
    helper.component(Breadcrumb).withProps(notePosition).render()
    await screen.findByText('Bazaar');
  });
});
