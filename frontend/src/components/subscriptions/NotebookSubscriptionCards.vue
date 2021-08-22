<template>
  <NotebookCardsWithButtons :notebooks="subscriptions.map(s=>s.notebook)">
    <template #default="{notebook}">
      <SubscriptionNoteButtons :subscription="subscriptions.find(s=>s.notebook === notebook)" @updated="$emit('updated')"/>
    </template>
  </NotebookCardsWithButtons>
</template>

<script>
import NotebookCardsWithButtons from "../notebook/NotebookCardsWithButtons.vue"
import SubscriptionNoteButtons from "./SubscriptionNoteButtons.vue"

export default {
  name: 'NotebookSubscriptionCards',
  props: { subscriptions: Array },
  emits: ['updated'],
  components: { NotebookCardsWithButtons, SubscriptionNoteButtons }
}
</script>
