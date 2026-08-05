<template>
  <li
    class="flex flex-col gap-2"
    :data-testid="`resolve-match-row-${matched.id}`"
  >
    <NoteTitleWithLink :note-topology="matched" />
    <div
      v-if="matchRealmRef"
      :data-testid="`resolve-match-path-${matched.id}`"
    >
      <BreadcrumbWithCircle
        :ancestor-folders="matchRealmRef.ancestorFolders ?? []"
        :notebook-realm="matchRealmRef.notebookRealm"
      />
    </div>
  </li>
</template>

<script setup lang="ts">
import type { PropType } from "vue"
import type { NoteTopology } from "@generated/doughnut-backend-api"
import NoteTitleWithLink from "@/components/notes/NoteTitleWithLink.vue"
import BreadcrumbWithCircle from "@/components/toolbars/BreadcrumbWithCircle.vue"
import { useStorageAccessor } from "@/composables/useStorageAccessor"

const props = defineProps({
  matched: {
    type: Object as PropType<NoteTopology>,
    required: true,
  },
})

const storageAccessor = useStorageAccessor()
const matchRealmRef = storageAccessor.value
  .storedApi()
  .getNoteRealmRefAndLoadWhenNeeded(props.matched.id)
</script>
