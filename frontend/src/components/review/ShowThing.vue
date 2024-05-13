<template>
  <div v-if="thing.linkType">
    <div class="jumbotron py-4 mb-2">
      <LinkShow v-bind="{ link: thing, storageAccessor }" />
    </div>
    <slot />
  </div>

  <div v-else-if="noteId">
    <NoteShow
      v-if="noteId"
      v-bind="{
        noteId,
        expandChildren: false,
        readonly: false,
        expandInfo,
        storageAccessor,
      }"
      :key="noteId"
      @level-changed="$emit('levelChanged', $event)"
      @self-evaluated="$emit('selfEvaluated', $event)"
    >
      <slot />
    </NoteShow>
  </div>
</template>

<script lang="ts">
import { defineComponent, PropType } from "vue";
import { Thing } from "@/generated/backend";
import LinkShow from "../links/LinkShow.vue";
import { StorageAccessor } from "../../store/createNoteStorage";

export default defineComponent({
  props: {
    thing: {
      type: Object as PropType<Thing>,
      required: true,
    },
    expandInfo: { type: Boolean, default: false },
    storageAccessor: {
      type: Object as PropType<StorageAccessor>,
      required: true,
    },
  },
  emits: ["levelChanged", "selfEvaluated"],
  components: { LinkShow },
  computed: {
    noteId() {
      return this.thing.note?.id;
    },
  },
});
</script>
