<template>

  <ModalWithButton v-model="show">
    <template #button>
      <button class="btn btn-small" title="link note" @click="show=true">
        <SvgLinkNote/>
      </button>
    </template>
    <template #header>
      <h3>Link <strong>{{noteTitle}}</strong> to</h3>
    </template>
    <template #body>
      <LinkNote v-bind="{noteId}" @done="done()"/>
    </template>
  </ModalWithButton>

</template>

<script>
import ModalWithButton from "../commons/ModalWithButton.vue"
import LinkNote from "./LinkNote.vue"
import SvgLinkNote from "../svgs/SvgLinkNote.vue"

export default {
  name: 'LinkNoteButton',
  props: { noteTitle: String, noteId: Number },
  emits: ['updated'],
  components: { ModalWithButton, LinkNote, SvgLinkNote },
  data() { return { show: false }},
  methods: {
    done() {
      this.show=false
      this.$emit('updated')
    }
  }
}

</script>
