<template>
  <span class="link-link">
    <LinkNob v-bind="{ note }" v-if="!!reverse" :inverse-icon="true" />
    <router-link
      :to="{ name: 'noteShow', params: { noteId: note.id } }"
      class="link-title text-decoration-none"
    >
      <NoteTopicComponent v-if="noteTopic" v-bind="{ noteTopic }" />
    </router-link>
    <LinkNob v-bind="{ note }" v-if="!reverse" :inverse-icon="false" />
  </span>
</template>

<script lang="ts">
import { defineComponent, PropType } from "vue";
import { Note } from "@/generated/backend";
import LinkNob from "./LinkNob.vue";
import { colors } from "../../colors";
import { StorageAccessor } from "../../store/createNoteStorage";
import NoteTopicComponent from "../notes/core/NoteTopic.vue";

export default defineComponent({
  props: {
    note: { type: Object as PropType<Note>, required: true },
    reverse: Boolean,
    storageAccessor: {
      type: Object as PropType<StorageAccessor>,
      required: true,
    },
  },
  components: { NoteTopicComponent, LinkNob },
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
