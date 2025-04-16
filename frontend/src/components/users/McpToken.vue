<template>
  <div class="daisy-container">
    <h2>MCP Token</h2>
    <div class="daisy-form-control">
      <input
        type="text"
        data-testid="mcp-token"
        :value="token"
        readonly
        class="daisy-input daisy-input-bordered"
      />
      <div class="daisy-flex daisy-gap-2 daisy-mt-4">
        <button
          class="daisy-btn daisy-btn-primary"
          @click="generateToken"
          data-testid="generate"
        >
          Generate
        </button>
        <button
          class="daisy-btn daisy-btn-error"
          @click="deleteToken"
          data-testid="delete"
        >
          Delete
        </button>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref } from "vue"
import useLoadingApi from "@/managedApi/useLoadingApi"
import { inject, type Ref } from "vue"
import type { User } from "@/generated/backend"

const { managedApi } = useLoadingApi()
const user = inject<Ref<User | undefined>>("currentUser")
if (!user?.value?.id) {
  throw new Error("User must be logged in")
}
const userId = user.value.id

const token = ref("")

const generateToken = async () => {
  token.value = "generated_token"
}

const deleteToken = async () => {
  token.value = ""
  await managedApi.restUserController.deleteUserToken(userId)
}
</script> 