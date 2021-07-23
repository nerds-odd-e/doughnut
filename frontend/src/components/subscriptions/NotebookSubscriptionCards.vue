<template>
  <NotebookCardsWithButtons :notebooks="subscriptions.map(s=>s.notebook)">
    <template #default="{notebook}">
      <SubscriptionNoteButtons :subscription="subscriptions.find(s=>s.notebook === notebook)"/>
    </template>
  </NotebookCardsWithButtons>
</template>

<script>
import NotebookCardsWithButtons from "../notebook/NotebookCardsWithButtons.vue"
import SubscriptionNoteButtons from "./SubscriptionNoteButtons.vue"

export default {
  name: 'NotebookSubscriptionCards',
  props: { subscriptions: Array },
  components: { NotebookCardsWithButtons, SubscriptionNoteButtons }
}
</script>
