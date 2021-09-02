<template>
  <slot name="button" />
  <Modal
    v-if="modelValue"
    @close_request="
      modelValue = false;
      $emit('update:modelValue', modelValue);
    "
  >
    <template v-slot:header>
      <slot name="header" />
    </template>
    <template v-slot:body>
      <slot name="body" />
    </template>
  </Modal>
</template>

<script>
import Modal from "./Modal.vue";
export default {
  name: "ModalWithButton",
  props: { modelValue: Boolean },
  emits: ["update:modelValue"],
  components: { Modal },
};
</script>
