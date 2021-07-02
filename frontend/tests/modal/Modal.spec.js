import Modal from '@/components/modal/Modal.vue';
import { mount } from '@vue/test-utils';

describe('Modal', () => {
  const TestComponent = {
    template: `
      <Modal @close_request="$emit('close_request')">
        <template v-slot:header>
        </template>
        <template v-slot:body>
        </template>
        <template v-slot:footer>
        </template>
      </Modal>
    `,
    components: { Modal },
    emits: ['close_request']
  }

  test('click on note when doing review', async () => {
    const wrapper = mount(TestComponent);
    expect(wrapper.find(".close-button").exists()).toBe(true)
    await wrapper.find(".close-button").trigger('click')
    console.log(wrapper.emitted())
    expect(wrapper.emitted().close_request).toHaveLength(1)
  });

});
