<template>
  <NoteCardWithHeaderAndButton :notes="subscriptions">
    <template #headerButtons="{note}">
      <SubscriptionNoteButtons :subscription="note"/>
    </template>
  </NoteCardWithHeaderAndButton>
</template>

<script>
import NoteCardWithHeaderAndButton from "../notes/NoteCardWithHeaderAndButton.vue"
import SubscriptionNoteButtons from "./SubscriptionNoteButtons.vue"

export default {
  name: 'NotebookSubscriptionCards',
  props: { subscriptions: Array },
  components: { NoteCardWithHeaderAndButton, SubscriptionNoteButtons }
}
</script>
