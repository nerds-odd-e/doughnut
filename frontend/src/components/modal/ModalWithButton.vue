<template>
  <slot name="button" :open="()=>showModal=true"/>
  <Modal v-if="showModal" @close_request="showModal=false">
    <template v-slot:header>
      <slot name="header"/>
    </template>
    <template v-slot:body>
      <slot name="body" :close="()=>showModal=false"/>
    </template>
  </Modal>
</template>

<script>
import Modal from "./Modal.vue"
export default {
  name: 'SearchNoteButton',
  components: { Modal },
    data() {
    return {
      showModal: false
    }
  }
}

</script>
