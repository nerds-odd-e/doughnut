<template>
  <Breadcrumb v-bind="{ noteTopology, ancestorFolders }">
    <template #topLink>
      <li v-if="fromBazaar">
        <router-link :to="{ name: 'bazaar' }">Bazaar</router-link>
      </li>
      <template v-else>
        <li>
          <router-link :to="{ name: 'notebooks' }">Notebooks</router-link>
        </li>
        <li v-if="notebook?.circle">
          <router-link
            :to="{
              name: 'circleShow',
              params: { circleId: notebook.circle.id },
            }"
            >{{ notebook.circle.name }}</router-link
          >
        </li>
      </template>
      <li v-if="notebook">
        <router-link
          v-if="notebook.id != null"
          :to="{
            name: 'notebookPage',
            params: { notebookId: String(notebook.id) },
          }"
          >{{ notebook.name }}</router-link
        >
        <template v-else>{{ notebook.name }}</template>
      </li>
    </template>
  </Breadcrumb>
</template>

<script setup lang="ts">
import type {
  Folder,
  Notebook,
  NoteTopology,
} from "@generated/doughnut-backend-api"
import type { PropType } from "vue"

defineProps({
  noteTopology: {
    type: Object as PropType<NoteTopology>,
    required: true,
  },
  ancestorFolders: {
    type: Array as PropType<Folder[]>,
    default: () => [],
  },
  fromBazaar: {
    type: Boolean,
    required: false,
  },
  notebook: {
    type: Object as PropType<Notebook>,
    required: false,
  },
})
</script>
