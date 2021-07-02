<template>
    <NoteRefreshable v-bind="{note, links, ancestors, notebook, owns}"/>
    <nav class="nav d-flex justify-content-between p-0 mb-2">
      <div class="btn-group btn-group-sm">
          <a  v-if="owns" :href="`/notes/${note.id}/move`">Move This Note</a>
      </div>
      <NoteNavigationButtons :navigation="navigation"/>
    </nav>
    <NoteOwnerViewCards :owns="owns" :notes="children"/>
</template>

<script>
import NoteRefreshable from "./NoteRefreshable.vue"
import NoteNavigationButtons from "./NoteNavigationButtons.vue"
import NoteControlHeader from "./NoteControlHeader.vue"
import NoteOwnerViewCards from "./NoteOwnerViewCards.vue"

export default {
  name: "NoteViewedByUser",
  props: {
    note: Object,
    links: Object,
    navigation: Object,
    children: Array,
    ancestors: Array,
    notebook: Object,
    owns: Boolean
  },
  components: { NoteRefreshable, NoteNavigationButtons, NoteControlHeader, NoteOwnerViewCards },
}
</script>
