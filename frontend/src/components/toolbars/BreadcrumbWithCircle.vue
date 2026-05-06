<template>
  <Breadcrumb v-bind="{ ancestorFolders }">
    <template #topLink>
      <li v-if="notebookView.readonly">
        <router-link :to="{ name: 'bazaar' }">Bazaar</router-link>
      </li>
      <template v-else>
        <li>
          <router-link :to="{ name: 'notebooks' }">Notebooks</router-link>
        </li>
        <li v-if="notebookView.notebook.circle">
          <router-link
            :to="{
              name: 'circleShow',
              params: { circleId: notebookView.notebook.circle.id },
            }"
            >{{ notebookView.notebook.circle.name }}</router-link
          >
        </li>
      </template>
      <li>
        <router-link
          v-if="notebookView.notebook.id != null"
          :to="{
            name: 'notebookPage',
            params: { notebookId: String(notebookView.notebook.id) },
          }"
          >{{ notebookView.notebook.name }}</router-link
        >
        <template v-else>{{ notebookView.notebook.name }}</template>
      </li>
    </template>
  </Breadcrumb>
</template>

<script setup lang="ts">
import type {
  Folder,
  NotebookClientView,
} from "@generated/doughnut-backend-api"
import type { PropType } from "vue"
import Breadcrumb from "@/components/toolbars/Breadcrumb.vue"

defineProps({
  ancestorFolders: {
    type: Array as PropType<Folder[]>,
    default: () => [],
  },
  notebookView: {
    type: Object as PropType<NotebookClientView>,
    required: true,
  },
})
</script>
