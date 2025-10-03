<template>
  <div class="daisy-container daisy-mx-auto daisy-p-4">
    <h1 class="daisy-text-2xl daisy-font-bold daisy-mb-4">Generate Token</h1>

    <PopButton ref="popbutton" btn-class="daisy-btn daisy-btn-primary daisy-btn-md">
      <template #button_face>
        Generate Token
      </template>
      <form @submit.prevent.once="generateToken">
        <TextInput
          scope-name="mcp-token"
          field="label"
          v-focus
          v-model="tokenFormData.label"
        />
        <input type="submit" value="Submit" class="daisy-btn daisy-btn-primary" />
      </form>
    </PopButton>

    <div v-if="token" class="daisy-flex daisy-items-center daisy-mt-6">
      <span class="daisy-mr-2 daisy-px-2 daisy-py-1 daisy-bg-base-200 daisy-rounded daisy-font-mono" data-testid="token-result">
        {{ token }}
      </span>
      <button
        class="daisy-btn daisy-btn-ghost daisy-btn-xs"
        @click="copyToken"
        :disabled="copied"
        aria-label="Copy token"
        data-testid="copy-token-btn"
      >
        <svg v-if="!copied" xmlns="http://www.w3.org/2000/svg" class="daisy-h-4 daisy-w-4" fill="none" viewBox="0 0 24 24" stroke="currentColor">
          <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M8 16h8M8 12h8m-7 8h6a2 2 0 002-2V6a2 2 0 00-2-2H8a2 2 0 00-2 2v12a2 2 0 002 2z" />
        </svg>
        <span v-else class="daisy-text-success">Copied!</span>
      </button>
    </div>

    <h2 class="daisy-text-xl daisy-font-bold daisy-mt-8">Existing Tokens</h2>
    <table class="daisy-table daisy-table-zebra daisy-mt-8 daisy-w-full">
      <thead>
        <tr>
          <th class="daisy-text-left daisy-px-4 daisy-py-2">Label</th>
          <th class="daisy-text-left daisy-px-4 daisy-py-2">Last Used</th>
          <th class="daisy-text-left daisy-px-4 daisy-py-2">Status</th>
        </tr>
      </thead>
      <tbody>
        <tr v-for="(item, idx) in tokens" :key="idx">
          <td class="daisy-px-4 daisy-py-2 daisy-font-mono">{{ item.label || 'No Label' }}</td>
          <td class="daisy-px-4 daisy-py-2 daisy-font-mono">{{ item.lastUsedAt || 'N/A' }}</td>
          <td class="daisy-px-4 daisy-py-2 daisy-font-mono">{{ item.isExpired ? 'Expired' : 'Valid' }}</td>
          <td class="daisy-px-4 daisy-py-2 daisy-font-mono">
            <div class="daisy-flex daisy-justify-end">
              <button class="daisy-btn daisy-btn-error daisy-btn-xs" @click="deleteToken(item.id)">Delete</button>
            </div>
          </td>
        </tr>
        <tr v-if="tokens.length === 0">
          <td class="daisy-px-4 daisy-py-2 daisy-text-gray-400">No tokens yet</td>
        </tr>
      </tbody>
    </table>
  </div>
</template>

<script setup lang="ts">
import { ref } from "vue"
import useLoadingApi from "@/managedApi/useLoadingApi"
import PopButton from "@/components/commons/Popups/PopButton.vue"
import TextInput from "@/components/form/TextInput.vue"

const { managedApi } = useLoadingApi()

const popbutton = ref<InstanceType<typeof PopButton> | null>(null)

const tokenFormData = ref({ label: "" })

const tokens = ref<
  Array<{ id: number; label: string; lastUsedAt: string | null; isExpired: boolean }>
>([])
const token = ref<string | null>(null)
const loading = ref(false)
const copied = ref(false)

const loadTokens = async () => {
  try {
    const res = await managedApi.restUserController.getTokens()
    tokens.value = res.map((t) => ({
      id: t.id,
      label: t.label,
      lastUsedAt: null,
      isExpired: t.isExpired,
    }))
  } catch (error) {
    console.error("Error loading tokens:", error)
  }
}

loadTokens()

const generateToken = async () => {
  loading.value = true
  copied.value = false
  try {
    const res = await managedApi.restUserController.generateToken({
      label: tokenFormData.value.label,
    })
    token.value = res.token
    tokens.value.push({ id: res.id, label: res.label, lastUsedAt: null, isExpired: res.isExpired })
    tokenFormData.value.label = ""
    popbutton.value?.closeDialog()
  } catch (error) {
    console.error("Error generating token:", error)
  } finally {
    loading.value = false
  }
}

const copyToken = async () => {
  if (token.value) {
    await navigator.clipboard.writeText(token.value)
    copied.value = true
    setTimeout(() => (copied.value = false), 1500)
  }
}

const deleteToken = async (id: number) => {
  try {
    await managedApi.restUserController.deleteToken(id)
    tokens.value = tokens.value.filter((token) => token.id !== id)
  } catch (error) {
    console.error("Error deleting token:", error)
  }
}
</script>
