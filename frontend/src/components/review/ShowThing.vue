<template>
  <main v-if="thing.linkType">
    <div class="jumbotron py-4 mb-2">
      <LinkShow v-bind="{ link: thing, storageAccessor }" />
    </div>
    <slot />
  </main>

  <main v-else-if="noteId">
    <NoteRealmLoader v-bind="{ noteId, storageAccessor }">
      <template #default="{ noteRealm }">
        <Breadcrumb
          v-if="noteRealm"
          v-bind="{ notePosition: noteRealm?.notePosition }"
        />
      </template>
    </NoteRealmLoader>
    <NoteShow
      v-if="noteId"
      v-bind="{
        noteId,
        expandChildren: false,
        readonly: false,
        storageAccessor,
      }"
    />
  </main>
</template>

<script lang="ts">
import { defineComponent, PropType } from "vue";
import { Thing } from "@/generated/backend";
import NoteRealmLoader from "../notes/NoteRealmLoader.vue";
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
  components: { LinkShow, NoteRealmLoader },
  computed: {
    noteId() {
      return this.thing.note?.id;
    },
  },
});
</script>
