<template>
  <span class="link-link">
    <LinkNob v-bind="{ noteTopology }" v-if="!!reverse" :inverse-icon="true" />
    <router-link
      :to="{ name: 'noteShow', params: { noteId: note.id } }"
      class="link-title text-decoration-none"
    >
      <NoteTopicComponent v-if="noteTopology" v-bind="{ noteTopology }" />
    </router-link>
    <LinkNob v-bind="{ noteTopology }" v-if="!reverse" :inverse-icon="false" />
  </span>
</template>

<script lang="ts">
import type { Note } from "@/generated/backend"
import type { PropType } from "vue"
import { defineComponent } from "vue"
import { colors } from "../../colors"
import type { StorageAccessor } from "../../store/createNoteStorage"
import NoteTopicComponent from "../notes/core/NoteTitleComponent.vue"
import LinkNob from "./LinkNob.vue"

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
    noteTopology() {
      return this.reverse
        ? this.note.noteTopology.parentOrSubjectNoteTopology!
        : this.note.noteTopology.objectNoteTopology!
    },
    fontColor() {
      return this.reverse ? colors.target : colors.source
    },
    colors() {
      return colors
    },
  },
})
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
