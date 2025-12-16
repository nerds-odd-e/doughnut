<template>
  <h2>How do you like this image from DALL-E?</h2>
  <form>
    <TextInput v-model="prompt" field="prompt" :error-message="promptError" />
    <div>
      <img class="ai-art" v-if="imageSrc" :src="imageSrc" />
    </div>
  </form>
  <button class="daisy-btn daisy-btn-secondary" @click="askForImage">Ask again</button>
</template>

<script setup lang="ts">
import type { Note } from "@generated/backend"
import { AiController } from "@generated/backend/sdk.gen"
import { apiCallWithLoading } from "@/managedApi/clientSetup"
import type { PropType } from "vue"
import { ref, computed, onMounted } from "vue"
import TextInput from "../form/TextInput.vue"

const props = defineProps({
  note: { type: Object as PropType<Note>, required: true },
})

const prompt = ref(props.note.noteTopology.title ?? "")
const b64Json = ref<string | undefined>(undefined)
const promptError = ref<string | undefined>(undefined)

const imageSrc = computed(() => {
  if (!b64Json.value) {
    return undefined
  }
  return `data:image/png;base64,${b64Json.value}`
})

const askForImage = async () => {
  const { data: imageResult, error } = await apiCallWithLoading(() =>
    AiController.generateImage({
      body: prompt.value,
    })
  )
  if (!error) {
    b64Json.value = imageResult!.b64encoded
    promptError.value = undefined
  } else {
    // Error is handled by global interceptor (toast notification)
    promptError.value = "There is a problem"
  }
}

onMounted(() => {
  askForImage()
})
</script>

<style lang="scss" scoped>
.ai-art {
  width: 100%;
  height: 100%;
}
</style>
