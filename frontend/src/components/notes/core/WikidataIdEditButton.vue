<template>
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
</template>

<script lang="ts">
import { Note } from "@/generated/backend"
import { StorageAccessor } from "@/store/createNoteStorage"
import { PropType, defineComponent } from "vue"
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
