<template>
  <ContainerPage
    v-bind="{
      contentExists: true,
      title: 'Feedback conversation',
    }"
  >
    <div class="chat-answer-container row mt-4 message-left">
      <div class="col">
        <div class="d-flex message-align">
          <div class="flex-column message-wrapper">
            <div class="message">
              I don't understand this question
            </div>
            <p>
              Name
            </p>
          </div>
        </div>
      </div>
    </div>

    <!-- Message for the other side -->
    <!-- <div class="chat-answer-container row mt-4 message-right">
      <div class="col">
        <div class="d-flex message-align">
          <div class="flex-column message-wrapper">
            <div class="message">
              Text message
            </div>
            <p>
              Name
            </p>
          </div>
        </div>
      </div>
    </div> -->

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
  left: 0;
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

.message-wrapper {
  max-width: 50%;
  min-width: 25%;

}

.message-right .message-align {
  justify-content: flex-end;
}

.message-right p {
  text-align: right;
}

.message-left .message-align {
  justify-content: flex-start;
}

.message {
  background-color: #f0f0f0;
  border-radius: 10px;
  word-wrap: break-word;
  white-space: pre-wrap;
  text-align: left;
  padding: 12px 16px;
}
</style>
