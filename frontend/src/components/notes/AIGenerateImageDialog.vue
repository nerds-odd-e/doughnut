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

<script lang="ts">
import type { Note } from "@generated/backend"
import { AiController } from "@generated/backend/sdk.gen"
import type { StorageAccessor } from "@/store/createNoteStorage"
import type { PropType } from "vue"
import { defineComponent } from "vue"
import TextInput from "../form/TextInput.vue"

export default defineComponent({
  props: {
    note: { type: Object as PropType<Note>, required: true },
    storageAccessor: {
      type: Object as PropType<StorageAccessor>,
      required: false,
    },
  },
  components: {
    TextInput,
  },
  data() {
    return {
      prompt: this.note.noteTopology.titleOrPredicate,
      b64Json: undefined as string | undefined,
      promptError: undefined as string | undefined,
    }
  },
  computed: {
    imageSrc() {
      if (!this.b64Json) {
        return undefined
      }
      return `data:image/png;base64,${this.b64Json}`
    },
  },
  methods: {
    async askForImage() {
      const { data: imageResult, error } = await AiController.generateImage({
        body: this.prompt,
      })
      if (!error) {
        this.b64Json = imageResult!.b64encoded
        this.promptError = undefined
      } else {
        // Error is handled by global interceptor (toast notification)
        this.promptError = "There is a problem"
      }
    },
  },
  mounted() {
    this.askForImage()
  },
})
</script>

<style lang="scss" scoped>
.ai-art {
  width: 100%;
  height: 100%;
}
</style>
