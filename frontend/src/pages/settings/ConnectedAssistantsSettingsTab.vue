<script setup lang="ts">
import { onMounted, ref } from "vue"
import type { AssistantManagement } from "@generated/donut-backend-api"
import { AssistantManagementController } from "@generated/donut-backend-api/sdk.gen"
import { apiCallWithLoading } from "@/managedApi/clientSetup"
import ContentLoader from "@/components/commons/ContentLoader.vue"

const management = ref<AssistantManagement>()
const loading = ref(false)
const failed = ref(false)

const loadAssistants = async () => {
  loading.value = true
  try {
    const { data, error } = await apiCallWithLoading(() =>
      AssistantManagementController.getConnectedAssistants()
    )
    failed.value = !!error
    if (!error) management.value = data
  } finally {
    loading.value = false
  }
}

const withdrawAccess = async (clientId: string) => {
  loading.value = true
  try {
    const { error } = await apiCallWithLoading(() =>
      AssistantManagementController.withdrawAssistant({
        path: { clientId },
        headers: {
          [management.value!.csrfHeaderName!]: management.value!.csrfToken!,
        },
      })
    )
    if (!error) await loadAssistants()
  } finally {
    loading.value = false
  }
}

onMounted(loadAssistants)
</script>

<template>
  <section>
    <h2 class="text-xl font-bold">Connected Assistants</h2>
    <p class="mt-2">These assistants can read notebooks you can currently read. Withdraw access to stop their access and automatic renewal.</p>
    <ContentLoader v-if="loading && !management" />
    <div v-else-if="failed" class="mt-6">
      <p>Unable to load connected assistants.</p>
      <button class="daisy-btn daisy-btn-outline mt-2" :disabled="loading" @click="loadAssistants">Try again</button>
    </div>
    <ul v-else-if="management?.assistants?.length" class="mt-6 divide-y divide-base-300">
      <li v-for="assistant in management.assistants" :key="assistant.clientId" class="flex flex-wrap items-center justify-between gap-3 py-4">
        <span class="min-w-0 break-words font-semibold">{{ assistant.clientName }}</span>
        <button class="daisy-btn daisy-btn-error daisy-btn-sm" :disabled="loading" :aria-label="`Withdraw access for ${assistant.clientName}`" @click="withdrawAccess(assistant.clientId!)">Withdraw access</button>
      </li>
    </ul>
    <p v-else-if="management" class="mt-6">No connected assistants.</p>
  </section>
</template>
