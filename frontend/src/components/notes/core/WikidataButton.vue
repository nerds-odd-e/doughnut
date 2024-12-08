<template>
  <div v-if="note.wikidataId" class="daisy-btn-group">
    <div class="daisy-dropdown">
      <button
        id="dropdownMenuButton"
        aria-expanded="false"
        aria-haspopup="true"
        class="daisy-btn daisy-btn-ghost"
        tabindex="0"
        role="button"
        title="wikidata options"
      >
        <SvgWikidata />
      </button>

      <div class="daisy-dropdown-content daisy-menu daisy-p-2 daisy-shadow daisy-bg-base-100 daisy-rounded-box">
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
import type { Note } from "@/generated/backend"
import type { StorageAccessor } from "@/store/createNoteStorage"
import type { PropType } from "vue"
import { defineComponent } from "vue"
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
