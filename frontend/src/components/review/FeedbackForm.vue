<template>
    <h2>Give Feedback</h2>
    <p>
        <i>
            If you believe that there is something wrong with this question please write it in the form below and submit
            it.
        </i>
    </p>
    <TextInput id="feedback-comment" field="comment" v-model="params.feedback"
        placeholder="Give feedback about the question" />
    <div class="feedback-actions-container">
        <button class="suggest-fine-tuning-ok-btn btn btn-success" @click="">
            OK
        </button>
    </div>
</template>

<script setup lang="ts">
import { QuizQuestion } from "@/generated/backend"
import { ref } from "vue"

interface FeedbackCreationParams {
  question: QuizQuestion | undefined
  feedback: string
}

const params = ref<FeedbackCreationParams>({
  question: undefined,
  feedback: "",
})

const emit = defineEmits(["closeDialog"])
</script>

<style scoped>
.feedback-btn {
    background-color: #007bff;
    color: white;
    padding: 5px;
    margin: 5px;
    border-radius: 5px;
}

.positive-feedback-btn.feedback-btn.selected {
    background-color: green;
}

.negative-feedback-btn.feedback-btn.selected {
    background-color: red;
}

.feedback-actions-container {
    display: flex;
    align-items: center;
}
</style>