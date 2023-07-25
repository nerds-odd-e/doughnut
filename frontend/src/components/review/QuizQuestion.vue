<template>
  <AIQuestion
    v-if="quizQuestion.questionType === 'AI_QUESTION'"
    :quiz-question="quizQuestion"
    :correct-choice-index="correctChoiceIndex"
    :answer-choice-index="answerChoiceIndex"
    @answer-to-ai-question="submitAnswer({ choiceIndex: $event })"
  />
  <template v-else>
    <div class="quiz-instruction inner-box">
      <ShowPicture
        v-if="quizQuestion.pictureWithMask"
        v-bind="quizQuestion.pictureWithMask"
        :opacity="1"
      />
      <NoteFrameOfLinks
        v-bind="{ links: quizQuestion.hintLinks, storageAccessor }"
      >
        <h2 v-if="!!quizQuestion.mainTopic" class="text-center">
          {{ quizQuestion.mainTopic }}
        </h2>
        <div
          class="quiz-description"
          v-if="quizQuestion.questionType !== 'PICTURE_TITLE'"
          v-html="quizQuestion.description"
        />
        <div v-if="quizQuestion.questionType === 'JUST_REVIEW'">
          <ReviewPointAsync
            v-if="reviewPointId"
            v-bind="{
              reviewPointId,
              storageAccessor,
            }"
          />
        </div>
      </NoteFrameOfLinks>
    </div>
    <div class="quiz-answering">
      <div v-if="quizQuestion.questionType === 'JUST_REVIEW'">
        <SelfEvaluateButtons
          @self-evaluated-memory-state="
            submitAnswer({ spellingAnswer: $event })
          "
          :key="reviewPointId"
        />
      </div>
      <div v-else-if="quizQuestion.questionType === 'SPELLING'">
        <form @submit.prevent.once="submitAnswer({ spellingAnswer: answer })">
          <TextInput
            scope-name="review_point"
            field="answer"
            v-model="answer"
            placeholder="put your answer here"
            v-focus
          />
          <input
            type="submit"
            value="OK"
            class="btn btn-primary btn-lg btn-block"
          />
        </form>
      </div>
      <div
        class="options"
        v-if="quizQuestion.options && quizQuestion.options.length > 0"
      >
        <div
          class="option"
          v-for="(option, index) in quizQuestion.options"
          :key="index"
        >
          <button
            class="btn btn-secondary btn-lg"
            @click.once="submitAnswer({ choiceIndex: index })"
          >
            <div v-if="!option.picture" v-html="option.display" />
            <div v-else>
              <ShowPicture v-bind="option.pictureWithMask" :opacity="1" />
            </div>
          </button>
        </div>
      </div>
    </div>
  </template>
</template>

<style scoped lang="sass">
.quiz-instruction
  height: 50%
  overflow-y: auto
.quiz-description
  white-space: pre-wrap
  height: 100%
  overflow: auto
.quiz-answering
  height: 50%
.options
  display: flex
  flex-wrap: wrap
  flex-direction: row
  justify-content: flex-start
  height: 100%
.option
  width: 46%
  margin: 2%
  @media(max-width: 500px)
    width: 100%
  button
    width: 100%
    height: 100%
    padding: 0
    display: flex
    justify-content: center
    align-items: center
    text-align: center
    border: 0
    border-radius: 0.5rem
    background-color: #e8e9ea
    color: #212529
    text-decoration: none
    white-space: normal
    word-break: break-word
    cursor: pointer
    transition: color 0.15s ease-in-out, background-color 0.15s ease-in-out, border-color 0.15s ease-in-out, box-shadow 0.15s ease-in-out
    &:hover
      color: #fff
      background-color: #007bff
      border-color: #007bff
    &:focus
      outline: 0
      box-shadow: 0 0 0 0.2rem rgba(0, 123, 255, 0.25)
    &:disabled
      opacity: 0.65
</style>

<script lang="ts">
import { defineComponent, PropType } from "vue";
import ShowPicture from "../notes/ShowPicture.vue";
import NoteFrameOfLinks from "../links/NoteFrameOfLinks.vue";
import TextInput from "../form/TextInput.vue";
import AIQuestion from "./AIQuestion.vue";
import ReviewPointAsync from "./ReviewPointAsync.vue";
import useLoadingApi from "../../managedApi/useLoadingApi";
import usePopups from "../commons/Popups/usePopups";
import SelfEvaluateButtons from "./SelfEvaluateButtons.vue";
import { StorageAccessor } from "../../store/createNoteStorage";

export default defineComponent({
  setup() {
    return { ...useLoadingApi(), ...usePopups() };
  },
  props: {
    quizQuestion: {
      type: Object as PropType<Generated.QuizQuestion>,
      required: true,
    },
    reviewPointId: {
      type: Number,
      required: false,
    },
    correctChoiceIndex: {
      type: Number,
      required: false,
    },
    answerChoiceIndex: {
      type: Number,
      required: false,
    },
    storageAccessor: {
      type: Object as PropType<StorageAccessor>,
      required: true,
    },
  },
  components: {
    ShowPicture,
    NoteFrameOfLinks,
    TextInput,
    ReviewPointAsync,
    SelfEvaluateButtons,
    AIQuestion,
  },
  emits: ["answered"],
  data() {
    return {
      answer: "" as string,
    };
  },
  methods: {
    async submitAnswer(answerData: Partial<Generated.Answer>) {
      try {
        const answerResult = await this.api.reviewMethods.processAnswer(
          this.quizQuestion.quizQuestionId,
          answerData,
        );
        this.$emit("answered", answerResult);
      } catch (_e) {
        await this.popups.alert(
          "This review point doesn't exist any more or is being skipped now. Moving on to the next review point...",
        );
      }
    },
  },
});
</script>
