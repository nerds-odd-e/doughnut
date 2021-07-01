<template>
    <NoteViewedByUserWithoutChildren v-bind="{note, links, ancestors, notebook, owns}"/>
    <nav class="nav d-flex justify-content-between p-0 mb-2">
      <div class="btn-group btn-group-sm">
          <a  v-if="owns" :href="`/notes/${note.id}/move`">Move This Note</a>
      </div>
      <NoteNavigationButtons :navigation="navigation"/>
    </nav>
    <NoteOwnerViewCards :owns="owns" :notes="children"/>
</template>

<script>
export default { name: "NoteViewedByUser" };
</script>

<script setup>
import NoteViewedByUserWithoutChildren from "./NoteViewedByUserWithoutChildren.vue"
import NoteNavigationButtons from "./NoteNavigationButtons.vue"
import NoteControlHeader from "./NoteControlHeader.vue"
import NoteOwnerViewCards from "./NoteOwnerViewCards.vue"

const props = defineProps({
  note: Object,
  links: Object,
  navigation: Object,
  children: Array,
  ancestors: Array,
  notebook: Object,
  owns: Boolean})
</script>
