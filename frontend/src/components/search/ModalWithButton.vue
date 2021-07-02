<template>
  <slot name="button" :open="()=>showModal=true"/>
  <Modal v-if="showModal">
    <template v-slot:header>
      <slot name="header"/>
    </template>
    <template v-slot:body>
      <slot name="body" :close="()=>showModal=false"/>
    </template>
    <template v-slot:footer>
      <slot name="footer"/>
    </template>
  </Modal>
</template>

<script>
import Modal from "./Modal.vue"
export default {
  name: 'SearchNoteButton',
  components: { Modal},
    data() {
    return {
      showModal: false
    }
  }
}

</script>
