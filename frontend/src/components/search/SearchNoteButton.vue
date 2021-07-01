<template>
  <ModalWithButton>
    <template #button="{open}">
      <button class="btn btn-small" title="link note" @click="open()">
        <SvgLinkNote/>
      </button>
    </template>
    <template v-slot:header>
      <h3>Link <strong>{{noteTitle}}</strong> to</h3>
    </template>
    <template v-slot:body>
      <SearchNote v-bind="{noteId}"/>
    </template>
  </ModalWithButton>
</template>

<script>
import ModalWithButton from "./ModalWithButton.vue"
import SearchNote from "./SearchNote.vue"
import SvgLinkNote from "../svgs/SvgLinkNote.vue"

export default {
  name: 'SearchNoteButton',
  props: { noteTitle: String, noteId: Number },
  components: { ModalWithButton, SearchNote, SvgLinkNote },
}

</script>
