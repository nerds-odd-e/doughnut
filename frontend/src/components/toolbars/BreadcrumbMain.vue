<template>
  <Breadcrumb
    v-bind="{
      ancestors: notePosition?.ancestors,
      notebook: notePosition?.notebook,
      circle: storageAccessor.circle,
    }"
  >
    <template #head>
      <PopButton title="choose a circle" :sidebar="true">
        <template #button_face>
          <SvgForward />
        </template>
        <CircleSelector />
      </PopButton>
    </template>

    <NoteNewButton
      v-if="parentId && user"
      v-bind="{ parentId, storageAccessor }"
      button-title="Add Sibling Note"
    >
      <SvgAddSibling />
    </NoteNewButton>
  </Breadcrumb>
</template>

<script lang="ts">
import { defineComponent, PropType } from "vue";
import NoteNewButton from "./NoteNewButton.vue";
import { StorageAccessor } from "../../store/createNoteStorage";
import Breadcrumb from "./Breadcrumb.vue";
import SvgAddSibling from "../svgs/SvgAddSibling.vue";
import PopButton from "../commons/Popups/PopButton.vue";
import SvgForward from "../svgs/SvgForward.vue";
import CircleSelector from "../circles/CircleSelector.vue";

export default defineComponent({
  props: {
    storageAccessor: {
      type: Object as PropType<StorageAccessor>,
      required: true,
    },
    user: { type: Object as PropType<Generated.User> },
  },
  components: {
    NoteNewButton,
    Breadcrumb,
    SvgAddSibling,
    PopButton,
    SvgForward,
    CircleSelector,
  },
  computed: {
    parentId() {
      if (!this.notePosition) return undefined;
      if (!this.storageAccessor.selectedNote) return undefined;
      const { ancestors } = this.notePosition;
      if (ancestors.length > 0) {
        return ancestors[ancestors.length - 1].id;
      }
      return undefined;
    },
    notePosition() {
      return this.storageAccessor.notePosition;
    },
  },
});
</script>
