<template>
  <ContestableQuestion
    v-if="quizQuestion"
    v-bind="{ selectedNote, quizQuestion, storageAccessor }"
  />
  <div v-show="answered" class="chat-answer-container">
    <img src="/user-icon.svg" class="chat-answer-icon" />
    <div class="chat-answer-text">
      <p id="chat-answer">{{ assistantMessage }}</p>
    </div>
  </div>
  <div ref="bottomOfTheChat" style="height: 140px; display: block"></div>

  <div class="chat-container">
    <div class="container">
      <button
        v-if="!quizQuestion"
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
import { defineComponent, PropType } from "vue";
import type { StorageAccessor } from "@/store/createNoteStorage";
import useLoadingApi from "../../managedApi/useLoadingApi";
import ContestableQuestion from "../review/ContestableQuestion.vue";
import scrollToElement from "../commons/scrollToElement";

export default defineComponent({
  setup() {
    return useLoadingApi();
  },
  props: {
    selectedNote: { type: Object as PropType<Generated.Note>, required: true },
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
      quizQuestion: undefined as Generated.QuizQuestion | undefined,
      chatInput: "",
      assistantMessage: "",
      answered: false,
    };
  },
  computed: {
    isButtonDisabled() {
      return this.chatInput === "";
    },
  },
  methods: {
    scrollToBottom() {
      const elm = this.$refs.bottomOfTheChat as HTMLElement;
      if (elm) {
        scrollToElement(elm);
      }
    },
    async generateQuestion() {
      this.quizQuestion = await this.api.quizQuestions.generateQuestion(
        this.selectedNote.id,
      );
      this.scrollToBottom();
    },
    async generateChatAnswer() {
      this.assistantMessage = await this.api.ai.chat(
        this.selectedNote.id,
        this.chatInput,
      );
      this.answered = true;
    },
  },
});
</script>

<style lang="scss" scoped>
.chat-container {
  position: fixed;
  width: 100%;
  padding-top: 5px;
  padding-bottom: 5px;
  bottom: 10px;
  right: 0;
  z-index: 1000;
}

span {
  display: block;
  overflow: hidden;
  padding-right: 5px;
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
