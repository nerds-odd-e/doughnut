<template>
  <div class="daisy-relative" style="z-index: 1001;">
    <PopButton title="associate wikidata">
      <template #button_face>
        <SvgWikidata />
        {{ title || "" }}
      </template>
      <template #default="{ closer }">
        <WikidataAssociationDialog
          v-bind="{ note, storageAccessor }"
          @close-dialog="closer"
        />
      </template>
    </PopButton>
  </div>
</template>

<script lang="ts">
import type { Note } from "@/generated/backend"
import type { StorageAccessor } from "@/store/createNoteStorage"
import type { PropType } from "vue"
import { defineComponent } from "vue"
import PopButton from "../../commons/Popups/PopButton.vue"
import SvgWikidata from "../../svgs/SvgWikidata.vue"
import WikidataAssociationDialog from "../WikidataAssociationDialog.vue"

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
    title: String,
  },
  components: {
    SvgWikidata,
    WikidataAssociationDialog,
    PopButton,
  },
})
</script>
