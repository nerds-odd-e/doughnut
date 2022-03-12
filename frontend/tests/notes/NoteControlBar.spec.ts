/**
 * @jest-environment jsdom
 */
import { screen } from '@testing-library/vue';
import Breadcrumb from '@/components/notes/Breadcrumb.vue';
import helper from '../helpers';
import makeMe from '../fixtures/makeMe';

describe('note wth child cards', () => {
  beforeEach(()=>{
    helper.reset()
  })

  it('view note belongs to other people in bazaar', async () => {
    const note = makeMe.aNoteSphere.please();
    const notePosition = makeMe.aNotePosition.inBazaar().please();
    helper.store.loadNoteSpheres([note]);
    helper.component(Breadcrumb).withProps(notePosition).render()
    await screen.findByText('Bazaar');
  });
});
