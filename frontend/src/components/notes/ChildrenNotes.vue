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
          <NoteShow
            v-if="openedNotes.includes(note.id)"
            v-bind="{
              noteId: note.id,
              storageAccessor,
              readonly,
              expandChildren: false,
            }"
          />
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

<script lang="ts">
import { PropType, defineComponent, ref } from "vue";
import { Note } from "@/generated/backend";
import { StorageAccessor } from "@/store/createNoteStorage";
import NoteTopic from "./core/NoteTopic.vue";
import SvgCollapse from "../svgs/SvgCollapse.vue";
import SvgExpand from "../svgs/SvgExpand.vue";

export default defineComponent({
  setup(props) {
    return {
      internalExpandChildren: ref(props.expandChildren),
    };
  },
  props: {
    notes: { type: Array as PropType<Note[]>, required: true },
    expandChildren: { type: Boolean, required: true },
    readonly: { type: Boolean, default: true },
    storageAccessor: {
      type: Object as PropType<StorageAccessor>,
      required: true,
    },
  },
  components: { NoteTopic, SvgCollapse, SvgExpand },
  data() {
    return {
      openedNotes: [] as number[],
    };
  },
  methods: {
    collapse() {
      this.internalExpandChildren = false;
    },
    expand() {
      this.internalExpandChildren = true;
    },
    highlight(noteId: number) {
      this.openedNotes.push(noteId);
    },
    navigateTo(noteId: number) {
      this.$router.push({ name: "noteShow", params: { noteId } });
    },
  },
});
</script>
