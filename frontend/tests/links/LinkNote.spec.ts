/**
 * @jest-environment jsdom
 */
import LinkNoteFinalize from '@/components/links/LinkNoteFinalize.vue';
import makeMe from '../fixtures/makeMe';
import { StoredComponentTestHelper } from '../helpers';

describe('LinkNoteFinalize', () => {
  let helper: StoredComponentTestHelper;

  beforeEach(() => {
    helper = new StoredComponentTestHelper();
  });

  it('going back', async () => {
    const note = makeMe.aNote.please();
    const wrapper = helper.component(LinkNoteFinalize).withProps({
        note,
        targetNote: note,
      }).mount()
    await wrapper.find('.go-back-button').trigger('click');
    expect(wrapper.emitted().goBack).toHaveLength(1);
  });
});
