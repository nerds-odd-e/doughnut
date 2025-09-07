<template>
  <span class="link-container daisy-inline-flex daisy-items-center daisy-gap-1">
    <LinkNob v-bind="{ noteTopology }" v-if="!!reverse" :inverse-icon="true" />
    <router-link
      :to="{ name: 'noteShow', params: { noteId: note.id } }"
      class="link-title daisy-no-underline daisy-font-medium hover:daisy-underline daisy-transition-all"
    >
      <NoteTitleComponent v-if="noteTopology" v-bind="{ noteTopology }" />
    </router-link>
    <LinkNob v-bind="{ noteTopology }" v-if="!reverse" :inverse-icon="false" />
  </span>
</template>

<script lang="ts">
import type { Note } from "@generated/backend"
import type { PropType } from "vue"
import { defineComponent } from "vue"
import { colors } from "../../colors"
import type { StorageAccessor } from "../../store/createNoteStorage"
import NoteTitleComponent from "../notes/core/NoteTitleComponent.vue"
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
  components: { NoteTitleComponent, LinkNob },
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
.link-title {
  color: v-bind(fontColor);
}
</style>
