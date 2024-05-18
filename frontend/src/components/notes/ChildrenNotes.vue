<template>
  <div v-if="notes.length > 0" class="row">
    <div class="col-auto bg-light p-0" style="width: 40px">
      <button
        class="btn btn-sm"
        v-if="internalExpandChildren"
        role="button"
        title="collapse children"
        @click="collapse()"
      >
        <SvgCollapse />
      </button>
      <button
        class="btn btn-sm"
        v-else
        role="button"
        title="expand children"
        @click="expand()"
      >
        <SvgExpand />
      </button>
    </div>
    <div class="col">
      <div class="row">
        <div v-if="!internalExpandChildren">
          <div role="collapsed-children-count">{{ notes.length }}</div>
        </div>
        <div v-else v-for="note in notes" :key="note.id">
          <NoteRealmLoader
            v-if="openedNotes.includes(note.id)"
            v-bind="{ noteId: note.id, storageAccessor }"
          >
            <template #default="{ noteRealm }">
              <NoteShowInner
                v-bind="{
                  noteRealm,
                  highlightNoteId,
                  expandChildren: false,
                  readonly,
                  storageAccessor,
                }"
              />
            </template>
          </NoteRealmLoader>

          <h5
            v-else
            class="card-title w-100"
            @click="highlight(note.id)"
            @dblclick="navigateTo(note.id)"
          >
            <NoteTopic v-bind="{ topic: note.topic }" />
          </h5>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { PropType, ref } from "vue";
import { useRouter } from "vue-router";
import { Note } from "@/generated/backend";
import { StorageAccessor } from "@/store/createNoteStorage";
import NoteTopic from "./core/NoteTopic.vue";
import SvgCollapse from "../svgs/SvgCollapse.vue";
import SvgExpand from "../svgs/SvgExpand.vue";

const props = defineProps({
  notes: { type: Array as PropType<Note[]>, required: true },
  expandChildren: { type: Boolean, required: true },
  readonly: { type: Boolean, required: true },
  highlightNoteId: { type: Number, required: true },
  storageAccessor: {
    type: Object as PropType<StorageAccessor>,
    required: true,
  },
});

const emit = defineEmits(["highlight-note"]);

const internalExpandChildren = ref(props.expandChildren);
const openedNotes = ref<number[]>([]);

const collapse = () => {
  internalExpandChildren.value = false;
};

const expand = () => {
  internalExpandChildren.value = true;
};

const highlight = (noteId: number) => {
  openedNotes.value.push(noteId);
  emit("highlight-note", noteId);
};

const navigateTo = (noteId: number) => {
  useRouter().push({ name: "noteShow", params: { noteId } });
};
</script>
