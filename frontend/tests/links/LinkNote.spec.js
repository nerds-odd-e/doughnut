import LinkNoteFinalize from '@/components/links/LinkNoteFinalize.vue';
import { mount } from '@vue/test-utils';
import {noteViewedByUser} from '../notes/fixtures'

describe('LinkNoteFinalize', () => {

  test('going back', async () => {
    const wrapper = mount(LinkNoteFinalize, {propsData: {noteId: 345, targetNote: noteViewedByUser.note}});
    await wrapper.find(".go-back-button").trigger("click")
    expect(wrapper.emitted().goBack).toHaveLength(1)

  });

});
