<template>
  <div
    role="dialog"
    aria-modal="true"
    aria-labelledby="notebook-catalog-move-to-group-title"
    class="daisy-px-1"
  >
    <h2
      id="notebook-catalog-move-to-group-title"
      class="daisy-m-0 daisy-mb-4 daisy-text-lg daisy-font-semibold"
    >
      Move to group
    </h2>
    <div v-if="loadError" class="daisy-text-error daisy-text-sm">
      {{ loadError }}
    </div>
    <form v-else class="daisy-flex daisy-flex-col daisy-gap-3" @submit.prevent="submit">
      <label class="daisy-form-control daisy-w-full">
        <span class="daisy-label-text">Target</span>
        <select
          id="notebook-catalog-move-to-group-target"
          v-model="selectedTarget"
          class="daisy-select daisy-select-bordered daisy-w-full"
          :disabled="loadingGroups"
        >
          <option disabled value="">Choose…</option>
          <option v-if="catalogGroupId != null" value="ungrouped">
            Ungrouped
          </option>
          <option
            v-for="g in targetGroups"
            :key="g.id"
            :value="`group:${g.id}`"
          >
            {{ g.name }}
          </option>
          <option value="new">Create new group…</option>
        </select>
      </label>
      <template v-if="selectedTarget === 'new'">
        <TextInput
          v-model="newGroupName"
          scope-name="notebookCatalogMoveToGroup"
          field="newGroupName"
          title="New group name"
          :error-message="newGroupError"
        />
      </template>
      <p v-if="submitError" class="daisy-m-0 daisy-text-sm daisy-text-error">
        {{ submitError }}
      </p>
      <div class="daisy-flex daisy-justify-end daisy-gap-2">
        <button
          type="button"
          class="daisy-btn daisy-btn-ghost daisy-btn-sm"
          @click="$emit('close')"
        >
          Cancel
        </button>
        <button
          type="submit"
          class="daisy-btn daisy-btn-primary daisy-btn-sm"
          :disabled="loadingGroups || applying"
        >
          Move
        </button>
      </div>
    </form>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, ref, watch } from "vue"
import type { NotebookCatalogGroupItem } from "@generated/doughnut-backend-api"
import {
  NotebookController,
  NotebookGroupController,
  SubscriptionController,
} from "@generated/doughnut-backend-api/sdk.gen"
import { apiCallWithLoading } from "@/managedApi/clientSetup"
import { toOpenApiError } from "@/managedApi/openApiError"
import { useToast } from "@/composables/useToast"
import type { NotebookCatalogEntry } from "@/components/notebook/patchNotebookInCatalogItems"
import TextInput from "@/components/form/TextInput.vue"

const props = defineProps<{
  mode: "owned" | "subscribed"
  notebookId: number
  subscriptionId?: number
  catalogGroupId?: number
  /** When set (including empty array), skip myNotebooks fetch and use these groups. */
  existingGroups?: { id: number; name: string }[]
  /** When set, createGroup includes circleId (circle ownership). */
  circleId?: number
}>()

const emit = defineEmits<{
  close: []
  success: []
}>()

const { showSuccessToast } = useToast()

const loadingGroups = ref(true)
const applying = ref(false)
const loadError = ref<string | null>(null)
const submitError = ref<string | null>(null)
const groups = ref<{ id: number; name: string }[]>([])
const selectedTarget = ref("")
const newGroupName = ref("")
const newGroupError = ref<string | undefined>(undefined)

const targetGroups = computed(() =>
  groups.value.filter((g) => g.id !== props.catalogGroupId)
)

watch(selectedTarget, () => {
  submitError.value = null
  newGroupError.value = undefined
})

async function loadGroupsFromMyNotebooks() {
  const { data, error } = await NotebookController.myNotebooks({})
  loadingGroups.value = false
  if (error || !data?.catalogItems) {
    loadError.value = "Could not load notebook groups."
    return
  }
  const items = data.catalogItems as NotebookCatalogEntry[]
  groups.value = items
    .filter((i): i is NotebookCatalogGroupItem => i.type === "notebookGroup")
    .map((g) => ({ id: g.id, name: g.name }))
}

onMounted(async () => {
  if (props.existingGroups !== undefined) {
    groups.value = props.existingGroups.map((g) => ({ ...g }))
    loadingGroups.value = false
    return
  }
  await loadGroupsFromMyNotebooks()
})

watch(
  () => props.existingGroups,
  (list) => {
    if (list !== undefined) {
      groups.value = list.map((g) => ({ ...g }))
    }
  },
  { deep: true }
)

async function applyNotebookGroup(notebookGroupId: number | undefined) {
  if (props.mode === "owned") {
    return apiCallWithLoading(() =>
      NotebookController.updateNotebookGroup({
        path: { notebook: props.notebookId },
        body: notebookGroupId === undefined ? {} : { notebookGroupId },
      })
    )
  }
  const subId = props.subscriptionId
  if (subId === undefined) {
    return { error: new Error("Missing subscription") }
  }
  return apiCallWithLoading(() =>
    SubscriptionController.updateSubscriptionGroup({
      path: { subscription: subId },
      body: notebookGroupId === undefined ? {} : { notebookGroupId },
    })
  )
}

const submit = async () => {
  submitError.value = null
  newGroupError.value = undefined
  const t = selectedTarget.value
  if (t === "") {
    submitError.value = "Choose a target."
    return
  }
  if (t === "new") {
    const trimmed = newGroupName.value.trim()
    if (!trimmed) {
      newGroupError.value = "Name is required"
      return
    }
    applying.value = true
    const { data: created, error: createErr } = await apiCallWithLoading(() =>
      NotebookGroupController.createGroup({
        body:
          props.circleId !== undefined
            ? { name: trimmed, circleId: props.circleId }
            : { name: trimmed },
      })
    )
    if (createErr || !created?.id) {
      applying.value = false
      const errObj = toOpenApiError(createErr)
      submitError.value =
        errObj.errors?.name ?? errObj.message ?? "Could not create group"
      return
    }
    const { error: patchErr } = await applyNotebookGroup(created.id)
    applying.value = false
    if (!patchErr) {
      showSuccessToast("Notebook group updated")
      emit("success")
    } else {
      submitError.value = toOpenApiError(patchErr).message ?? "Could not move"
    }
    return
  }
  if (t === "ungrouped") {
    applying.value = true
    const { error } = await applyNotebookGroup(undefined)
    applying.value = false
    if (!error) {
      showSuccessToast("Notebook group updated")
      emit("success")
    } else {
      submitError.value = toOpenApiError(error).message ?? "Could not move"
    }
    return
  }
  const m = /^group:(\d+)$/.exec(t)
  if (!m) {
    submitError.value = "Invalid selection."
    return
  }
  const gid = Number.parseInt(m[1]!, 10)
  applying.value = true
  const { error } = await applyNotebookGroup(gid)
  applying.value = false
  if (!error) {
    showSuccessToast("Notebook group updated")
    emit("success")
  } else {
    submitError.value = toOpenApiError(error).message ?? "Could not move"
  }
}
</script>
