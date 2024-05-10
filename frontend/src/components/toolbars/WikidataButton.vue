<template>
  <div class="dropdown">
    <PopButton title="associate wikidata">
      <template #button_face>
        <SvgWikidata />
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
import { defineComponent, PropType } from "vue";
import { StorageAccessor } from "@/store/createNoteStorage";
import { Note } from "@/generated/backend";
import SvgWikidata from "../svgs/SvgWikidata.vue";
import WikidataAssociationDialog from "../notes/WikidataAssociationDialog.vue";
import PopButton from "../commons/Popups/PopButton.vue";

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
    WikidataAssociationDialog,
    PopButton,
  },
});
</script>
