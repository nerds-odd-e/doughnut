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
      <CopyButton
        :text="token"
        btn-class="daisy-btn daisy-btn-ghost daisy-btn-xs"
        aria-label="Copy token"
        test-id="copy-token-btn"
      />
    </div>

    <h2 class="daisy-text-xl daisy-font-bold daisy-mt-8">Existing Tokens</h2>
    <table class="daisy-table daisy-table-zebra daisy-mt-8 daisy-w-full">
      <thead>
        <tr>
          <th class="daisy-text-left daisy-px-4 daisy-py-2">Label</th>
        </tr>
      </thead>
      <tbody>
        <tr v-for="(item, idx) in tokens" :key="idx">
          <td class="daisy-px-4 daisy-py-2 daisy-font-mono">{{ item.label || 'No Label' }}</td>
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
import CopyButton from "@/components/commons/CopyButton.vue"

const { managedApi } = useLoadingApi()

const popbutton = ref<InstanceType<typeof PopButton> | null>(null)

const tokenFormData = ref({ label: "" })

const tokens = ref<
  Array<{
    id: number
    label: string
  }>
>([])
const token = ref<string | null>(null)
const loading = ref(false)

const loadTokens = async () => {
  try {
    const res = await managedApi.services.getTokens()
    tokens.value = res.map((t) => ({
      id: t.id,
      label: t.label,
    }))
  } catch (error) {
    console.error("Error loading tokens:", error)
  }
}

loadTokens()

const generateToken = async () => {
  loading.value = true
  try {
    const res = await managedApi.services.generateToken({
      body: {
        label: tokenFormData.value.label,
      },
    })
    token.value = res.token
    tokens.value.push({
      id: res.id,
      label: res.label,
    })
    tokenFormData.value.label = ""
    popbutton.value?.closeDialog()
  } catch (error) {
    console.error("Error generating token:", error)
  } finally {
    loading.value = false
  }
}

const deleteToken = async (id: number) => {
  try {
    await managedApi.services.deleteToken({ path: { tokenId: id } })
    tokens.value = tokens.value.filter((token) => token.id !== id)
  } catch (error) {
    console.error("Error deleting token:", error)
  }
}
</script>
