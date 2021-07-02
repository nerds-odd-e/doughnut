<template>
  <ModalWithButton>
    <template #button="{open}">
      <button class="btn btn-small" title="link note" @click="open()">
        <SvgLinkNote/>
      </button>
    </template>
    <template #header>
      <h3>Link <strong>{{noteTitle}}</strong> to</h3>
    </template>
    <template #body="{close}">
      <LinkNote v-bind="{noteId}" @done="close();$emit('updated')"/>
    </template>
  </ModalWithButton>
</template>

<script>
import ModalWithButton from "../search/ModalWithButton.vue"
import LinkNote from "./LinkNote.vue"
import SvgLinkNote from "../svgs/SvgLinkNote.vue"

export default {
  name: 'LinkNoteButton',
  props: { noteTitle: String, noteId: Number },
  emits: ['updated'],
  components: { ModalWithButton, LinkNote, SvgLinkNote },
}

</script>
