<template>
  <nav class="navbar navbar-expand-lg navbar-light bg-light">
    <div class="container-fluid d-flex">
      <button
        role="button"
        class="d-lg-none btn btn-sm"
        title="toggle sidebar"
        @click="$emit('toggle-sidebar')"
      >
        <span class="navbar-toggler-icon"></span>
      </button>
      <div class="d-flex flex-grow-1 justify-content-between">
        <Breadcrumb
          v-if="noteRealm"
          v-bind="{ notePosition: noteRealm?.notePosition }"
        />
        <div class="btn-group">
          <span class="btn btn-sm" role="button" title="edit note">
            <SvgEdit />
          </span>
        </div>
      </div>
    </div>
  </nav>
</template>

<script setup lang="ts">
import { PropType, computed, toRefs } from "vue";
import Breadcrumb from "../toolbars/Breadcrumb.vue";
import { StorageAccessor } from "../../store/createNoteStorage";

const props = defineProps({
  noteId: { type: Number, required: true },
  storageAccessor: {
    type: Object as PropType<StorageAccessor>,
    required: true,
  },
});

const emit = defineEmits(["toggle-sidebar"]);

const reactiveProps = toRefs(props);

const noteRealmRef = computed(() =>
  reactiveProps.storageAccessor.value
    .storedApi()
    .getNoteRealmRef(reactiveProps.noteId.value),
);

const noteRealm = computed(() => noteRealmRef.value?.value);
</script>
