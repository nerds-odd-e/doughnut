<template>
    <NoteViewedByUserWithoutChildren v-bind="{note, links, ancestors, notebook, owns}" @updated="$emit('updated')"/>
    <nav class="nav d-flex justify-content-between p-0 mb-2">
      <div class="btn-group btn-group-sm">
      </div>
      <NoteNavigationButtons :navigation="navigation"/>
    </nav>
    <NoteOwnerViewCards :owns="owns" :notes="children" @updated="$emit('updated')"/>
</template>

<script>
import NoteViewedByUserWithoutChildren from "./NoteViewedByUserWithoutChildren.vue"
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
    owns: { type: Boolean, required: true }
  },
  emits: ['updated'],
  components: { NoteViewedByUserWithoutChildren, NoteNavigationButtons, NoteControlHeader, NoteOwnerViewCards },
}
</script>
