<template>
  <div v-if="isOwner" class="notebook-group-assignment daisy-mt-4 daisy-w-full daisy-max-w-md">
    <label class="daisy-label daisy-w-full" for="notebook-group-select">
      <span class="daisy-label-text">Notebook group</span>
    </label>
    <select
      id="notebook-group-select"
      v-model="selectedGroupId"
      class="daisy-select daisy-select-bordered daisy-w-full"
    >
      <option value="">Ungrouped</option>
      <option v-for="g in groups" :key="g.id" :value="String(g.id)">
        {{ g.name }}
      </option>
    </select>
    <button
      type="button"
      class="daisy-btn daisy-btn-primary daisy-btn-sm daisy-mt-2"
      @click="save"
    >
      Save notebook group
    </button>
  </div>
</template>

<script setup lang="ts">
import type { PropType } from "vue"
import { computed, onMounted, ref, watch } from "vue"
import type {
  Notebook,
  NotebookCatalogGroupItem,
  User,
} from "@generated/doughnut-backend-api"
import { NotebookController } from "@generated/doughnut-backend-api/sdk.gen"
import { apiCallWithLoading } from "@/managedApi/clientSetup"
import { useToast } from "@/composables/useToast"
import type { NotebookCatalogEntry } from "@/components/notebook/patchNotebookInCatalogItems"

const props = defineProps({
  notebook: { type: Object as PropType<Notebook>, required: true },
  user: { type: Object as PropType<User>, required: false },
})

const emit = defineEmits<{
  (e: "notebook-updated", notebook: Notebook): void
}>()

const { showSuccessToast } = useToast()

const isOwner = computed(
  () => props.user?.externalIdentifier === props.notebook.creatorId
)

const groups = ref<{ id: number; name: string }[]>([])
const selectedGroupId = ref("")

function findGroupIdForNotebook(
  items: NotebookCatalogEntry[],
  notebookId: number
): string {
  for (const item of items) {
    if (item.type === "notebookGroup") {
      const grp = item as NotebookCatalogGroupItem
      if (grp.notebooks.some((n) => n.id === notebookId)) {
        return String(grp.id)
      }
    }
  }
  return ""
}

const loadCatalog = async () => {
  const { data, error } = await NotebookController.myNotebooks({})
  if (error || !data?.catalogItems) return
  const items = data.catalogItems
  groups.value = items
    .filter((i): i is NotebookCatalogGroupItem => i.type === "notebookGroup")
    .map((g) => ({ id: g.id, name: g.name }))
  selectedGroupId.value = findGroupIdForNotebook(items, props.notebook.id)
}

onMounted(() => {
  if (isOwner.value) {
    loadCatalog()
  }
})

watch(
  () => props.notebook.id,
  () => {
    if (isOwner.value) {
      loadCatalog()
    }
  }
)

const save = async () => {
  const raw = selectedGroupId.value
  const notebookGroupId =
    raw === "" ? null : (Number.parseInt(raw, 10) as number)
  const { data: updated, error } = await apiCallWithLoading(() =>
    NotebookController.updateNotebookGroup({
      path: { notebook: props.notebook.id },
      body: { notebookGroupId: notebookGroupId ?? undefined },
    })
  )
  if (!error && updated) {
    emit("notebook-updated", updated)
    showSuccessToast("Notebook group updated")
    await loadCatalog()
  }
}
</script>
