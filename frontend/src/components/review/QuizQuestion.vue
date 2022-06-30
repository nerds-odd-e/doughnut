<template>
  <BasicBreadcrumb :ancestors="quizQuestion.scope" />
  <ShowPicture
    v-if="quizQuestion.pictureWithMask"
    v-bind="quizQuestion.pictureWithMask"
    :opacity="1"
  />
  <NoteFrameOfLinks v-bind="{ links: quizQuestion.hintLinks }">
    <div class="quiz-instruction">
      <pre
        style="white-space: pre-wrap"
        v-if="quizQuestion.questionType !== 'PICTURE_TITLE'"
        v-html="quizQuestion.description"
      />
      <h2 v-if="!!quizQuestion.mainTopic" class="text-center">
        {{ quizQuestion.mainTopic }}
      </h2>
    </div>
  </NoteFrameOfLinks>

  <div v-if="quizQuestion.questionType === 'JUST_REVIEW'">
    <ReviewPointAsync
      v-bind="{
        reviewPointId: quizQuestion.quizQuestion.reviewPoint,
      }"
    />
    <div class="btn-group">
      <button
        class="btn btn-primary"
        @click.once="sumbitAnswer({ spellingAnswer: 'yes' })"
      >
        Yes, I remember
      </button>
      <button
        class="btn btn-secondary"
        @click.once="sumbitAnswer({ spellingAnswer: 'no' })"
      >
        No, I need more repetition
      </button>
    </div>
  </div>
  <div v-else-if="quizQuestion.questionType === 'SPELLING'">
    <form @submit.prevent.once="sumbitAnswer({ spellingAnswer: answer })">
      <div class="aaa">
        <TextInput
          scope-name="review_point"
          field="answer"
          v-model="answer"
          placeholder="put your answer here"
          :autofocus="true"
        />
      </div>
      <input
        type="submit"
        value="OK"
        class="btn btn-primary btn-lg btn-block"
      />
    </form>
  </div>
  <div class="row" v-else>
    <div
      class="col-sm-6 mb-3 d-grid"
      v-for="option in quizQuestion.options"
      :key="option.noteId"
    >
      <button
        class="btn btn-secondary btn-lg"
        @click.once="sumbitAnswer({ answerNoteId: option.noteId })"
      >
        <div v-if="!option.picture" v-html="option.display" />
        <div v-else>
          <ShowPicture v-bind="option.pictureWithMask" :opacity="1" />
        </div>
      </button>
    </div>
  </div>
</template>

<script lang="ts">
import { defineComponent, PropType } from "vue";
import BasicBreadcrumb from "../commons/BasicBreadcrumb.vue";
import ShowPicture from "../notes/ShowPicture.vue";
import NoteFrameOfLinks from "../links/NoteFrameOfLinks.vue";
import TextInput from "../form/TextInput.vue";
import ReviewPointAsync from "./ReviewPointAsync.vue";

export default defineComponent({
  props: {
    quizQuestion: {
      type: Object as PropType<Generated.QuizQuestionViewedByUser>,
      required: true,
    },
  },
  components: {
    BasicBreadcrumb,
    ShowPicture,
    NoteFrameOfLinks,
    TextInput,
    ReviewPointAsync,
  },
  emits: ["answer", "removeFromReview"],
  data() {
    return {
      answer: "" as string,
    };
  },
  methods: {
    sumbitAnswer(answerData: Partial<Generated.Answer>) {
      this.$emit("answer", answerData);
    },
  },
});
</script>
