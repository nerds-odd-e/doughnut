<template>
  <div>
    <PopButton
      ref="popbutton"
      btn-class="daisy-btn daisy-btn-primary daisy-btn-md"
    >
      <template #button_face>
        Generate Token
      </template>
      <form @submit.prevent.once="generateToken">
        <TextInput
          scope-name="access-token"
          field="label"
          v-focus
          v-model="tokenFormData.label"
        />
        <input
          type="submit"
          value="Submit"
          :disabled="loading"
          class="daisy-btn daisy-btn-primary"
        />
      </form>
    </PopButton>

    <div v-if="token" class="flex items-center mt-6">
      <span
        class="mr-2 px-2 py-1 bg-base-200 rounded font-mono"
        data-testid="token-result"
      >
        {{ token }}
      </span>
      <CopyButton
        :text="token"
        btn-class="daisy-btn daisy-btn-ghost daisy-btn-xs"
        aria-label="Copy token"
        test-id="copy-token-btn"
      />
    </div>

    <h2 class="text-xl font-bold mt-8">Existing Tokens</h2>
    <table class="daisy-table daisy-table-zebra mt-8 w-full">
      <thead>
        <tr>
          <th class="text-left px-4 py-2">Label</th>
          <th class="text-right px-4 py-2">Actions</th>
        </tr>
      </thead>
      <tbody>
        <tr v-for="(item, idx) in tokens" :key="idx">
          <td class="px-4 py-2 font-mono">{{ item.label || "No Label" }}</td>
          <td class="px-4 py-2 font-mono">
            <div class="flex justify-end">
              <button
                class="daisy-btn daisy-btn-error daisy-btn-xs"
                :disabled="loading"
                @click="deleteToken(item)"
              >
                Delete
              </button>
            </div>
          </td>
        </tr>
        <tr v-if="tokens.length === 0">
          <td colspan="2" class="px-4 py-2 text-gray-400">No tokens yet</td>
        </tr>
      </tbody>
    </table>
  </div>
</template>

<script setup lang="ts">
import { ref } from "vue"
import { UserController } from "@generated/doughnut-backend-api/sdk.gen"
import { apiCallWithLoading } from "@/managedApi/clientSetup"
import usePopups from "@/components/commons/Popups/usePopups"
import PopButton from "@/components/commons/Popups/PopButton.vue"
import TextInput from "@/components/form/TextInput.vue"
import CopyButton from "@/components/commons/CopyButton.vue"

const { popups } = usePopups()

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
  const { data: tokensList, error } = await UserController.getTokens({})
  if (!error) {
    tokens.value = tokensList!.map((t) => ({
      id: t.id,
      label: t.label,
    }))
  }
}

loadTokens()

const generateToken = async () => {
  loading.value = true
  try {
    const { data: newToken, error } = await apiCallWithLoading(() =>
      UserController.generateToken({
        body: {
          label: tokenFormData.value.label,
        },
      })
    )
    if (!error) {
      token.value = newToken!.token
      tokens.value.push({
        id: newToken!.id,
        label: newToken!.label,
      })
      tokenFormData.value.label = ""
      popbutton.value?.closeDialog()
    }
  } finally {
    loading.value = false
  }
}

const deleteToken = async (item: { id: number; label: string }) => {
  const label = item.label || "this token"
  if (
    !(await popups.confirm(
      `Delete access token "${label}"? Any API clients using it will stop working.`
    ))
  ) {
    return
  }
  loading.value = true
  try {
    const { error } = await apiCallWithLoading(() =>
      UserController.deleteToken({ path: { tokenId: item.id } })
    )
    if (!error) {
      tokens.value = tokens.value.filter((t) => t.id !== item.id)
    }
  } finally {
    loading.value = false
  }
}
</script>
