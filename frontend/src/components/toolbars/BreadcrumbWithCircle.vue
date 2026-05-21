<template>
  <Breadcrumb
    v-bind="{
      ancestorFolders,
      breadcrumbNotebookId: notebookRealm.notebook.id,
    }"
  >
    <template #topLink>
      <li v-if="notebookRealm.readonly">
        <router-link :to="{ name: 'bazaar' }">Bazaar</router-link>
      </li>
      <template v-else>
        <li v-if="notebookRealm.notebook.circle">
          <router-link
            :to="{
              name: 'circleShow',
              params: { circleId: notebookRealm.notebook.circle.id },
            }"
            >{{ notebookRealm.notebook.circle.name }}</router-link
          >
        </li>
      </template>
      <li>
        <router-link
          class="inline-flex items-center gap-1 text-base-content"
          :to="{
            name: 'notebookPage',
            params: { notebookId: String(notebookRealm.notebook.id) },
          }"
        >
          <BookText class="h-3.5 w-3.5 shrink-0" aria-hidden="true" />
          {{ notebookRealm.notebook.name }}
        </router-link>
      </li>
    </template>
  </Breadcrumb>
</template>

<script setup lang="ts">
import type { Folder, NotebookRealm } from "@generated/doughnut-backend-api"
import type { PropType } from "vue"
import { BookText } from "@lucide/vue"
import Breadcrumb from "@/components/toolbars/Breadcrumb.vue"

defineProps({
  ancestorFolders: {
    type: Array as PropType<Folder[]>,
    default: () => [],
  },
  notebookRealm: {
    type: Object as PropType<NotebookRealm>,
    required: true,
  },
})
</script>
