<template>
    <div>
      <h2>Send message to bazaar</h2>
      <p>
        <i>
          Let's discuss about this note!
        </i>
      </p>
      <TextArea
        field="message"
        v-model="message"
        placeholder="Send message about the question"
        :rows="5"
      />
    <button class="btn btn-success" @click="submitMessage">
      Submit
    </button>
    </div>
</template>

<script setup lang="ts">
import { ref } from "vue"
import useLoadingApi from "@/managedApi/useLoadingApi.ts"

const { managedApi } = useLoadingApi()
const message = ref<string>("")
const props = defineProps<{
  noteId: number
}>()
const emit = defineEmits(["submitted"])

async function submitMessage() {
  await managedApi.restNoteController.sendNoteFeedback(
    props.noteId,
    message.value
  )

  emit("submitted")
}
</script>
