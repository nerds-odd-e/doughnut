<template>
  <span class="link-link">
    <LinkNob
      v-bind="{ note, colors, storageAccessor }"
      v-if="!!reverse"
      :inverse-icon="true"
    />
    <NoteTopicWithLink
      v-if="noteTopic"
      class="link-title"
      v-bind="{ noteTopic }"
    />
    <LinkNob
      v-bind="{ note, colors, storageAccessor }"
      v-if="!reverse"
      :inverse-icon="false"
    />
  </span>
</template>

<script lang="ts">
import { defineComponent, PropType } from "vue";
import { Note } from "@/generated/backend";
import NoteTopicWithLink from "../notes/NoteTopicWithLink.vue";
import LinkNob from "./LinkNob.vue";
import { colors } from "../../colors";
import { StorageAccessor } from "../../store/createNoteStorage";

export default defineComponent({
  props: {
    note: { type: Object as PropType<Note>, required: true },
    reverse: Boolean,
    storageAccessor: {
      type: Object as PropType<StorageAccessor>,
      required: true,
    },
  },
  components: { NoteTopicWithLink, LinkNob },
  computed: {
    noteTopic() {
      return this.reverse
        ? this.note.noteTopic.parentNoteTopic
        : this.note.noteTopic.targetNoteTopic;
    },
    fontColor() {
      return this.reverse ? colors.target : colors.source;
    },
    colors() {
      return colors;
    },
  },
});
</script>

<style scoped>
.link-link {
  padding-bottom: 3px;
  margin-right: 10px;
}

.link-title {
  padding-bottom: 3px;
  color: v-bind(fontColor);
}
</style>
