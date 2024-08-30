<template>
  <ContainerPage
    v-bind="{
      contentExists: true,
      title: 'Feedback conversation',
    }"
  >
    <h2>Empty conversation</h2>
    <div class="mt-4" />
  <!-- <ContestableQuestion /> -->
    <div class="chat-answer-container row message">
      <!-- <div v-if="message.role === 'assistant'" class="assistant-icon col-auto"> -->
    </div>

    <div class="col">
      <!-- <div v-if="message.role === 'assistant'" v-html="markdowntToHtml(message.content?.[0]?.text?.value)" /> -->
      <!-- <div v-else class="d-flex justify-content-end"> -->
      <div class="d-flex justify-content-end">
        <div class="user-message">
          I think the question is wrong
        </div>
      </div>
    </div>

    <div ref="bottomOfTheChat" style="height: 140px; display: block"></div>

    <div class="chat-controls">
      <div class="container">
        <form class="chat-input-container">
          <TextArea ref="chatInputTextArea" class="flex-grow-1" id="chat-input" :rows="1" :auto-extend-until="5" :enter-submit="true" />
          <input type="submit" value="Chat" id="chat-button" class="btn float-btn btn-secondary" />
        </form>
      </div>
    </div>
  </ContainerPage>
</template>

<script setup lang="ts">
import ContainerPage from "@/pages/commons/ContainerPage.vue"
</script>

<style>
.chat-controls {
  position: fixed;
  bottom: 0;
  width: 100%;
  background-color: white;
  box-shadow: 0 -2px 4px rgba(0, 0, 0, 0.1);
}

.chat-input-container {
  width: 100%;
  display: flex;
  flex-direction: row;
  padding-top: 5px;
  padding-bottom: 5px;
}

input.auto-extendable-input {
  width: 100%;
}

.message {
  background-color: #f0f0f0;
  border-radius: 10px;
  max-width: 70%;
  word-wrap: break-word;
  white-space: pre-wrap;
  text-align: left;
  padding: 12px 16px;
}
</style>