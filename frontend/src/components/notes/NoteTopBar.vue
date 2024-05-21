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
        <NoteRealmLoader v-bind="{ noteId: currentNoteId.id, storageAccessor }">
          <template #default="{ noteRealm }">
            <Breadcrumb
              v-if="noteRealm"
              v-bind="{ notePosition: noteRealm?.notePosition }"
            />
          </template>
        </NoteRealmLoader>
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
  storageAccessor: {
    type: Object as PropType<StorageAccessor>,
    required: true,
  },
});

const emit = defineEmits(["toggle-sidebar"]);

const reactiveProps = toRefs(props);

const currentNoteIdRef = computed(() =>
  reactiveProps.storageAccessor.value.currentNoteIdRef(),
);
const currentNoteId = computed(() => currentNoteIdRef.value?.value);
</script>
