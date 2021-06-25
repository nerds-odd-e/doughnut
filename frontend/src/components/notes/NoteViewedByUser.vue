<template>
    <NoteControlHeader v-if="owns" :note="note" :ancestors="ancestors" :notebook="notebook"/>
    <NoteBazaarBreadcrumb v-else :ancestors="ancestors">
      <li class="breadcrumb-item">{{note.title}}</li>
    </NoteBazaarBreadcrumb>
    <div class="jumbotron py-4 mb-2">
        <nav class="nav d-flex flex-row-reverse p-0">
            <NoteButtons v-if="owns" :note="note"/>
            <BazaarNoteButtons v-else :note="note" :notebook="notebook" />
        </nav>
        <NoteShow :note="note" :links="links" :level="1" :owns="owns"/>
    </div>
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
import NoteShow from "./NoteShow.vue"
import NoteBazaarBreadcrumb from "../bazaar/NoteBazaarBreadcrumb.vue"
import BazaarNoteButtons from "../bazaar/BazaarNoteButtons.vue"
import NoteButtons from "./NoteButtons.vue"
import NoteNavigationButtons from "./NoteNavigationButtons.vue"
import NoteControlHeader from "./NoteControlHeader.vue"
import NoteOwnerViewCards from "./NoteOwnerViewCards.vue"
import ContentLoader from "../ContentLoader.vue"
import LoadingThinBar from "../LoadingThinBar.vue"

const props = defineProps({
  note: Object,
  links: Object,
  navigation: Object,
  children: Array,
  ancestors: Array,
  notebook: Object,
  owns: Boolean})
</script>
