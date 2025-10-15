<template>
  <ContainerPage v-bind="{ contentLoaded: notebooks !== undefined, title: 'Notebooks' }">
    <div class="daisy-mb-6 daisy-flex daisy-items-center daisy-gap-10">
      <NotebookNewButton>Add New Notebook</NotebookNewButton>
      <p class="daisy-flex daisy-items-center daisy-gap-3">
      <span>MCP Notebook:</span>
      <select
        v-if="notebooks"
        class="daisy-select daisy-select-bordered daisy-select-md daisy-max-w-sm"
        :value="selectedMcpNotebookId ?? ''"
        @change="onSelectMcp(($event.target as HTMLSelectElement).value)"
        data-testid="mcp-select"
        title="Select MCP Notebook"
      >
        <option
          value=""
          disabled
          hidden
          :selected="selectedMcpNotebookId == null"
        >
          Select your MCP Notebook…
        </option>
        <option
          v-for="nb in notebooks"
          :key="nb.id"
          :value="nb.id"
        >
          {{ nb.title }}
        </option>
      </select>
      <button
        v-if="notebooks"
        class="daisy-btn daisy-btn-ghost daisy-btn-sm"
        @click="clearMcpSelection"
        data-testid="mcp-clear"
        title="Clear MCP selection"
      >
        Clear MCP Tag
      </button>
      </p>
    </div>
    <main>
      <NotebookCardsWithButtons v-if="notebooks" :notebooks="notebooks">
        <template #default="{ notebook }">
            <span
              v-if="notebook.notebookSettings?.selectMCPNotebook" 
              class="daisy-badge daisy-badge-primary daisy-badge-lg"
              title="This notebook is selected as MCP Notebook"
              data-testid="mcp-badge"
            >
              MCP
            </span>
            <span v-else></span>
          <NotebookButtons v-bind="{ notebook, user }" />
        </template>
      </NotebookCardsWithButtons>
    </main>
    <h2>Subscribed Notes</h2>
    <NotebookCardsWithButtons v-if="subscriptions" :notebooks="subscriptions?.map((s) => s.notebook!)">
      <template #default="{ notebook }">
        <SubscriptionNoteButtons
          :subscription="subscriptions.find((s) => s.notebook === notebook)"
          @updated="fetchData()"
        />
      </template>
    </NotebookCardsWithButtons>
  </ContainerPage>
</template>

<script setup lang="ts">
import { inject, onMounted, ref, type Ref } from "vue"
import type { Notebook, Subscription, User } from "@generated/backend"
import useLoadingApi from "@/managedApi/useLoadingApi"
import NotebookNewButton from "@/components/notebook/NotebookNewButton.vue"
import NotebookCardsWithButtons from "@/components/notebook/NotebookCardsWithButtons.vue"
import NotebookButtons from "@/components/notebook/NotebookButtons.vue"
import SubscriptionNoteButtons from "@/components/subscriptions/SubscriptionNoteButtons.vue"
import ContainerPage from "./commons/ContainerPage.vue"

const { managedApi } = useLoadingApi()

const user = inject<Ref<User | undefined>>("currentUser")
const subscriptions = ref<Subscription[] | undefined>(undefined)
const notebooks = ref<Notebook[] | undefined>(undefined)
const selectedMcpNotebookId = ref<string | number | undefined>(undefined)

const fetchData = async () => {
  const res = await managedApi.restNotebookController.myNotebooks()
  notebooks.value = res.notebooks
  subscriptions.value = res.subscriptions
  const current = notebooks.value?.find(
    (n) => n.notebookSettings?.selectMCPNotebook
  )
  selectedMcpNotebookId.value = current?.id
}
onMounted(() => {
  fetchData()
})

const onSelectMcp = async (value: string) => {
  const selectedId = value ? Number(value) : null
  const currentId =
    selectedMcpNotebookId.value == null
      ? null
      : Number(selectedMcpNotebookId.value)
  if (selectedId == null) {
    if (currentId != null) {
      await managedApi.restNotebookController.update1(currentId, {
        selectMCPNotebook: false,
      })
      await fetchData()
    }
    return
  }
  if (currentId != null) {
    await managedApi.restNotebookController.update1(currentId, {
      selectMCPNotebook: false,
    })
  }
  await managedApi.restNotebookController.update1(selectedId, {
    selectMCPNotebook: true,
  })
  await fetchData()
}

const clearMcpSelection = async () => {
  await onSelectMcp("")
}
</script>
