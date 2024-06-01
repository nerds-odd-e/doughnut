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
      <div v-if="!internalExpandChildren">
        <div role="collapsed-children-count">{{ notes.length }}</div>
      </div>
      <Cards v-else :notes="notes.map((n) => n.noteTopic)" :columns="2" />
    </div>
  </div>
</template>

<script setup lang="ts">
import { PropType, ref } from "vue";
import { Note } from "@/generated/backend";
import { StorageAccessor } from "@/store/createNoteStorage";
import SvgCollapse from "../svgs/SvgCollapse.vue";
import SvgExpand from "../svgs/SvgExpand.vue";

const props = defineProps({
  notes: { type: Array as PropType<Note[]>, required: true },
  expandChildren: { type: Boolean, required: true },
  readonly: { type: Boolean, required: true },
  storageAccessor: {
    type: Object as PropType<StorageAccessor>,
    required: true,
  },
});

const internalExpandChildren = ref(props.expandChildren);

const collapse = () => {
  internalExpandChildren.value = false;
};

const expand = () => {
  internalExpandChildren.value = true;
};
</script>
