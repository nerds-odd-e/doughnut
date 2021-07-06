<template>
  <NotebookCardsWithButtons v-bind="{notebooks}">
    <template #default="{notebook}">
      <NotebookButtons v-bind="{notebook}" class="card-header-btn"/>
      <slot class="card-header-btn" />
    </template>
  </NotebookCardsWithButtons>
</template>

<script>
import NotebookCardsWithButtons from "./NotebookCardsWithButtons.vue"
import NotebookButtons from "./NotebookButtons.vue"

export default {
  name: 'NotebookViewCards',
  props: {notebooks: Array},
  components: { NotebookCardsWithButtons, NotebookButtons }
}
</script>
