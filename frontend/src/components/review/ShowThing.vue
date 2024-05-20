<template>
  <div v-if="thing.linkType">
    <main class="jumbotron py-4 mb-2">
      <LinkShow v-bind="{ link: thing, storageAccessor }" />
    </main>
    <slot />
  </div>

  <div v-else-if="noteId">
    <NoteShow
      v-if="noteId"
      v-bind="{
        noteId,
        expandChildren: false,
        readonly: false,
        storageAccessor,
      }"
    />
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
    storageAccessor: {
      type: Object as PropType<StorageAccessor>,
      required: true,
    },
  },
  components: { LinkShow },
  computed: {
    noteId() {
      return this.thing.note?.id;
    },
  },
});
</script>
