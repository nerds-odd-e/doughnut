<template>

<SearchNote v-if="!targetNote" v-bind="{noteId}" @selected="targetNote=$event"/>
<LinkNoteFinalize v-else v-bind="{targetNote, noteId}" @success="$emit('done')" @goBack="targetNote=null"/>

</template>

<script>
import LinkNoteFinalize from "./LinkNoteFinalize.vue"
import SearchNote from "../search/SearchNote.vue"

export default {
  name: 'LinkNote',
  props: { noteId: String },
  components: {LinkNoteFinalize, SearchNote},
  emits: [ 'done' ],
  data() {
    return {
      targetNote: null,
    }
  },
}

</script>
