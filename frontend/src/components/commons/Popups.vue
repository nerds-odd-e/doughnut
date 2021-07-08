<template>
  <Modal v-if="popupInfo && popupInfo.type==='confirm'" @close_request="resolve(false)">
    <template v-slot:header>
      <h2> Please confirm </h2>
    </template>
    <template v-slot:body>
      <div>
      <span>{{popupInfo.message}}</span>
      </div>
      <button class="btn btn-success" v-on:click="resolve(true)">OK</button>
      <button class="btn btn-secondary" v-on:click="resolve(false)">Cancel</button>
    </template>
  </Modal>
</template>

<script>
import Modal from "./Modal.vue"
export default {
  name: 'ModalWithButton',
  props: { popupInfo: Object },
  emits: ['done'],
  components: { Modal },
  methods: {
    resolve(result) {
      this.$emit('done', result)
    }
  }
}

</script>
