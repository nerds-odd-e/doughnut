<template>
  <span class="link-nob">
    <PopButton v-if="note.linkType" :title="note.linkType">
      <template #button_face>
        <SvgLinkTypeIcon
          :link-type="note.linkType"
          :inverse-icon="inverseIcon"
        />
      </template>
      <template #default="{ closer }">
        <LinkNobDialog
          v-bind="{ note, inverseIcon, colors, storageAccessor }"
          @close-dialog="closer"
        />
      </template>
    </PopButton>
  </span>
</template>

<script lang="ts">
import { defineComponent, PropType } from "vue";
import { Note } from "@/generated/backend";
import SvgLinkTypeIcon from "../svgs/SvgLinkTypeIcon.vue";
import PopButton from "../commons/Popups/PopButton.vue";
import LinkNobDialog from "./LinkNobDialog.vue";
import { StorageAccessor } from "../../store/createNoteStorage";

export default defineComponent({
  props: {
    note: {
      type: Object as PropType<Note>,
      required: true,
    },
    storageAccessor: {
      type: Object as PropType<StorageAccessor>,
      required: true,
    },
    inverseIcon: Boolean,
    colors: Object,
  },
  components: { SvgLinkTypeIcon, PopButton, LinkNobDialog },
});
</script>
