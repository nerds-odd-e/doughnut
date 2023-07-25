<template>
  <div class="quiz-instruction">
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
      <QuizQuestionChoices
        v-if="quizQuestion.options"
        :choices="quizQuestion.options"
        :correct-choice-index="correctChoiceIndex"
        :answer-choice-index="answerChoiceIndex"
        :disabled="disabled"
        @answer="submitAnswer($event)"
      />
    </div>
  </div>
</template>

<style scoped lang="sass">
.quiz-description
  white-space: pre-wrap
  height: 100%
  overflow: auto
.quiz-answering
  height: 50%
</style>

<script lang="ts">
import { defineComponent, PropType } from "vue";
import ShowPicture from "../notes/ShowPicture.vue";
import NoteFrameOfLinks from "../links/NoteFrameOfLinks.vue";
import TextInput from "../form/TextInput.vue";
import ReviewPointAsync from "./ReviewPointAsync.vue";
import useLoadingApi from "../../managedApi/useLoadingApi";
import usePopups from "../commons/Popups/usePopups";
import SelfEvaluateButtons from "./SelfEvaluateButtons.vue";
import QuizQuestionChoices from "./QuizQuestionChoices.vue";
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
    reviewPointId: Number,
    correctChoiceIndex: Number,
    answerChoiceIndex: Number,
    disabled: Boolean,
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
    QuizQuestionChoices,
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
