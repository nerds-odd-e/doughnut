<template>
  <div class="btn-group btn-group-sm">
    <template v-if="!selectedNote">
      <PopButton title="search note">
        <template #button_face>
          <SvgSearch />
        </template>
        <LinkNoteDialog v-bind="{ storageAccessor }" />
      </PopButton>
    </template>
    <NoteControlCenter
      v-if="selectedNote"
      v-bind="{ selectedNote, storageAccessor }"
    />
  </div>
</template>

<script lang="ts">
import { defineComponent, PropType } from "vue";
import useLoadingApi from "@/managedApi/useLoadingApi";
import { StorageAccessor } from "@/store/createNoteStorage";
import NoteControlCenter from "./NoteControlCenter.vue";
import SvgSearch from "../svgs/SvgSearch.vue";
import LinkNoteDialog from "../links/LinkNoteDialog.vue";
import PopButton from "../commons/Popups/PopButton.vue";

export default defineComponent({
  setup() {
    return {
      ...useLoadingApi(),
    };
  },
  props: {
    storageAccessor: {
      type: Object as PropType<StorageAccessor>,
      required: true,
    },
    user: { type: Object as PropType<Generated.User> },
  },
  emits: ["updateUser"],
  components: {
    NoteControlCenter,
    SvgSearch,
    LinkNoteDialog,
    PopButton,
  },
  computed: {
    selectedNote(): Generated.Note | undefined {
      return this.storageAccessor.selectedNote;
    },
  },
});
</script>
