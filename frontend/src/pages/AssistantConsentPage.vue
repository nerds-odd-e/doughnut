<script setup lang="ts">
import { computed, onMounted, ref } from "vue"
import type { ConsentContext } from "@generated/donut-backend-api"
import { AssistantConsentController } from "@generated/donut-backend-api/sdk.gen"
import { apiCallWithLoading } from "@/managedApi/clientSetup"
import ContainerPage from "@/pages/commons/ContainerPage.vue"
import { namedLocationHref } from "@/routes/namedLocationHref"

const props = defineProps<{ clientId: string; state: string }>()
const consent = ref<ConsentContext>()
const failed = ref(false)
const canRecord = computed(() =>
  consent.value?.scopes?.includes("learning_session:record")
)

onMounted(async () => {
  const { data, error } = await apiCallWithLoading(() =>
    AssistantConsentController.getAssistantConsentContext({
      query: { client_id: props.clientId, state: props.state },
    })
  )
  if (error) failed.value = true
  else consent.value = data
})
</script>

<template>
  <ContainerPage title="Connect an assistant" :content-loaded="!!consent || failed">
    <div class="mx-auto max-w-xl daisy-card bg-base-100 border border-base-300">
      <div v-if="failed" class="daisy-card-body">
        <p>Unable to load this authorization request. Return to your assistant and start connecting again.</p>
      </div>
      <div v-else-if="consent" class="daisy-card-body gap-4">
        <h1 class="daisy-card-title">Allow {{ consent.clientName }} to read your notebooks{{ canRecord ? " and record learning sessions" : "" }}?</h1>
        <p>Donut account: <strong>{{ consent.accountName }}</strong></p>
        <p>This assistant can search, read, and explore notebooks that this account can currently read.</p>
        <p v-if="canRecord">This assistant can also record Learning Session Reports, saving Feedback and updating learning schedules for completed items.</p>
        <p>Access can continue through automatic renewal until you withdraw permission.</p>
        <p>You can withdraw this assistant’s access at any time in <a :href="namedLocationHref({ name: 'settingsConnectedAssistants' })" class="daisy-link">Settings</a>.</p>
        <div class="flex flex-wrap gap-3">
          <form v-for="decision in ['Allow', 'Deny']" :key="decision" :action="consent.formAction" method="post">
            <input type="hidden" name="client_id" :value="consent.clientId" />
            <input type="hidden" name="state" :value="consent.state" />
            <input type="hidden" :name="consent.csrfParameterName" :value="consent.csrfToken" />
            <template v-if="decision === 'Allow'">
              <input v-for="scope in consent.scopes" :key="scope" type="hidden" name="scope" :value="scope" />
            </template>
            <button type="submit" class="daisy-btn" :class="decision === 'Allow' ? 'daisy-btn-primary' : 'daisy-btn-outline'">{{ decision }}</button>
          </form>
        </div>
      </div>
    </div>
  </ContainerPage>
</template>
