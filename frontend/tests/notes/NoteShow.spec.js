import { render } from '@testing-library/vue'
import NoteShow from '@/components/notes/NoteShow.vue'
import {note} from './NoteShow-fixtures'

xdescribe('new/updated pink banner', () => {
  test('should show pink banner', async () => {
    const wrapper = render(NoteShow, {props: note})

    expect(await wrapper.findByText('This is a new thing')).toBeTruthy()
  });
});
