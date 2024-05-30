<template>
  <main v-if="note.noteTopic.linkType">
    <div class="jumbotron py-4 mb-2">
      <div class="row">
        <div class="col-12 col-md-4 link-source">
          <LinkNoteShow
            v-if="note.noteTopic.parentNoteTopic"
            :note-topic="note.noteTopic.parentNoteTopic"
          />
        </div>
        <div class="col-12 col-md-4 text-center">
          <LinkNob v-bind="{ noteTopic: note.noteTopic }" />
          <span class="badge bg-light text-dark">
            {{ note.noteTopic.linkType }}</span
          >
        </div>
        <div class="col-12 col-md-4 link-target">
          <LinkNoteShow
            v-if="note.noteTopic.targetNoteTopic"
            :note-topic="note.noteTopic.targetNoteTopic"
          />
        </div>
      </div>
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
import { Note } from "@/generated/backend";
import NoteRealmLoader from "../notes/NoteRealmLoader.vue";
import LinkNoteShow from "../links/LinkNoteShow.vue";
import LinkNob from "../links/LinkNob.vue";
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
  },
  components: { NoteRealmLoader },
  computed: {
    noteId() {
      return this.note.id;
    },
  },
});
</script>
