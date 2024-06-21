<template>
  <ContestableQuestion
    v-if="quizQuestionInNotebook"
    v-bind="{ quizQuestionInNotebook, storageAccessor }"
    @need-scroll="scrollToBottom"
  />
  <div
    class="chat-answer-container"
    v-for="(message, index) in assistantMessage"
    :class="message.role"
    :key="index"
  >
    <img src="/user-icon.svg" class="chat-answer-icon" />
    <div class="chat-answer-text">
      <p>{{ message.content?.[0]?.text?.value }}</p>
    </div>
  </div>
  <div ref="bottomOfTheChat" style="height: 140px; display: block"></div>

  <div class="chat-controls">
    <div class="container">
      <button
        v-if="!quizQuestionInNotebook"
        class="btn btn-secondary"
        @click="generateQuestion"
      >
        Test me
      </button>
      <form class="chat-input-container" @submit.prevent="generateChatAnswer">
        <input id="chat-input" class="chat-input-text" v-model="chatInput" />
        <input
          :disabled="isButtonDisabled"
          type="submit"
          value="Chat"
          id="chat-button"
          class="btn float-btn btn-secondary"
        />
      </form>
      <p class="text-center">
        <i
          >Ask question to AI about this note. Each question ignores the chat
          history, unlike ChatGPT.</i
        >
      </p>
    </div>
  </div>
</template>

<script lang="ts">
import { Message, Note, QuizQuestionInNotebook } from "@/generated/backend"
import useLoadingApi from "@/managedApi/useLoadingApi"
import type { StorageAccessor } from "@/store/createNoteStorage"
import { PropType, defineComponent } from "vue"
import scrollToElement from "../commons/scrollToElement"
import ContestableQuestion from "../review/ContestableQuestion.vue"

export default defineComponent({
  setup() {
    return useLoadingApi()
  },
  props: {
    selectedNote: { type: Object as PropType<Note>, required: true },
    storageAccessor: {
      type: Object as PropType<StorageAccessor>,
      required: true,
    },
  },
  components: {
    ContestableQuestion,
  },
  data() {
    return {
      quizQuestionInNotebook: undefined as QuizQuestionInNotebook | undefined,
      chatInput: "",
      assistantMessage: [] as Message[],
    }
  },
  computed: {
    isButtonDisabled() {
      return this.chatInput === ""
    },
    threadId() {
      return this.assistantMessage?.[this.assistantMessage.length - 1]?.thread_id
    },
  },
  methods: {
    scrollToBottom() {
      const elm = this.$refs.bottomOfTheChat as HTMLElement
      if (elm) {
        scrollToElement(elm)
      }
    },
    async generateQuestion() {
      this.quizQuestionInNotebook =
        await this.managedApi.restQuizQuestionController.generateQuestion(
          this.selectedNote.id,
        )
      this.scrollToBottom()
    },
    async generateChatAnswer() {
      this.assistantMessage.push({
        role: "user",
        content: [{ text: { value: this.chatInput } }],
      })
      this.assistantMessage = [...this.assistantMessage, ...(
        await this.managedApi.restAiController.chat(this.selectedNote.id, {
          userMessage: this.chatInput,
          threadId: this.threadId,
        })
      ).messages!]
    },
  },
})
</script>

<style lang="scss" scoped>
span {
  display: block;
  overflow: hidden;
  padding-right: 5px;
}

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

.chat-input-text {
  width: 100%;
  margin-right: 5px;
  flex-grow: 1;
}
input.auto-extendable-input {
  width: 100%;
}

.float-btn {
  float: right;
}

.chat-answer-container {
  display: flex;
  margin: 2% 0;
}

.chat-answer-icon {
  width: 6%;
  height: 6%;
}

.chat-answer-text {
  position: relative;
  display: inline-block;
  margin-left: 15px;
  padding: 7px 10px;
  width: 100%;
  border: solid 3px #555;
  box-sizing: border-box;
}

.chat-answer-text:before {
  content: "";
  position: absolute;
  top: 50%;
  left: -24px;
  margin-top: -12px;
  border: 12px solid transparent;
  border-right: 12px solid #fff;
  z-index: 2;
}

.chat-answer-text:after {
  content: "";
  position: absolute;
  top: 50%;
  left: -30px;
  margin-top: -14px;
  border: 14px solid transparent;
  border-right: 14px solid #555;
}

.chat-answer-text p {
  margin: 0;
  word-break: break-word;
}
</style>
