<template>
    <NoteControlHeader v-if="owns" :note="note" :ancestors="ancestors" :ownership="ownership"/>
    <NoteBazaarBreadcrumb v-else :ancestors="ancestors">
      <li class="breadcrumb-item">{{note.title}}</li>
    </NoteBazaarBreadcrumb>
    <div class="jumbotron py-4 mb-2">
        <nav class="nav d-flex flex-row-reverse p-0">
            <NoteButtons v-if="owns" :note="note"/>
            <BazaarNotebookButtons v-else :notebook="notebook" />
        </nav>
        <NoteShow :note="note" :links="links" :level="1" :forBazaar="!owns"/>
    </div>
    <nav class="nav d-flex justify-content-between p-0 mb-2">
      <div class="btn-group btn-group-sm">
          <a :href="`/notes/${note.id}/move`">Move This Note</a>
      </div>
      <NoteNavigationButtons :urlPrefix="''" :navigation="navigation"/>
    </nav>
    <NoteOwnerViewCards :notes="children"/>
</template>

<script setup>
import NoteShow from "./NoteShow.vue"
import NoteBazaarBreadcrumb from "../bazaar/NoteBazaarBreadcrumb.vue"
import BazaarNotebookButtons from "../notebook/BazaarNotebookButtons.vue"
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
  ownership: Object,
  ancestors: Array,
  notebook: Object,
  owns: Boolean})
</script>
