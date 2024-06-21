<template>
  <div v-if="note.wikidataId" class="btn-group">
    <div class="dropdown">
      <button
        id="dropdownMenuButton"
        aria-expanded="false"
        aria-haspopup="true"
        class="btn dropdown-toggle"
        data-bs-toggle="dropdown"
        role="button"
        title="wikidata options"
      >
        <SvgWikidata />
      </button>

      <div class="dropdown-menu">
        <NoteWikidataAssociation :wikidata-id="note.wikidataId" />
        <WikidataIdEditButton
          v-bind="{ note, storageAccessor }"
          title="Edit Wikidata Id"
        />
      </div>
    </div>
  </div>
  <WikidataIdEditButton v-else v-bind="{ note, storageAccessor }" />
</template>

<script lang="ts">
import { Note } from "@/generated/backend"
import { StorageAccessor } from "@/store/createNoteStorage"
import { PropType, defineComponent } from "vue"
import SvgWikidata from "../../svgs/SvgWikidata.vue"
import WikidataIdEditButton from "./WikidataIdEditButton.vue"

export default defineComponent({
  props: {
    storageAccessor: {
      type: Object as PropType<StorageAccessor>,
      required: true,
    },
    note: {
      type: Object as PropType<Note>,
      required: true,
    },
  },
  components: {
    SvgWikidata,
    WikidataIdEditButton,
  },
})
</script>
