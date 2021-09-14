<template>
  <NoteBreadcrumbForReview :ancestors="quizQuestion.scope" />
  <div v-if="pictureQuestion">
    <ShowPicture :note="sourceNote.note" :opacity="1" />
  </div>
  <LinkLists v-bind="{ links: quizQuestion.hintLinks, owns: true, staticInfo }">
    <div class="quiz-instruction">
      <pre
        style="white-space: pre-wrap"
        v-if="!pictureQuestion"
        v-html="quizQuestion.description"
      />
      <h2 v-if="!!quizQuestion.mainTopic" class="text-center">
        {{ quizQuestion.mainTopic }}
      </h2>
    </div>
  </LinkLists>

  <div class="row" v-if="quizQuestion.questionType !== 'SPELLING'">
    <div
      class="col-sm-6 mb-3 d-grid"
      v-for="option in quizQuestion.options"
      :key="option.note.id"
    >
      <button
        class="btn btn-secondary btn-lg"
        v-on:click.once="
          emptyAnswer.answerNoteId = option.note.id;
          processForm();
        "
      >
        <div v-if="!option.picture">{{ option.display }}</div>
        <div v-else>
          <ShowPicture :note="option.note" :opacity="1" />
        </div>
      </button>
    </div>
  </div>

  <div v-else>
    <form @submit.prevent.once="processForm">
      <div class="aaa">
        <TextInput
          scopeName="review_point"
          field="answer"
          v-model="emptyAnswer.answer"
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
</template>

<script setup>
import NoteBreadcrumbForReview from "./NoteBreadcrumbForReview.vue";
import ShowPicture from "../notes/ShowPicture.vue";
import LinkLists from "../links/NoteFrameOfLinks.vue";
import TextInput from "../form/TextInput.vue";
import { computed } from "@vue/runtime-core";

const props = defineProps({
  reviewPointViewedByUser: Object,
  quizQuestion: Object,
  emptyAnswer: Object,
  staticInfo: Object,
});
const emits = defineEmits(["answer"]);
const sourceNote = computed(() => {
  if (!!props.reviewPointViewedByUser.noteViewedByUser)
    return props.reviewPointViewedByUser.noteViewedByUser;
  return props.reviewPointViewedByUser.linkViewedByUser.sourceNoteViewedByUser;
});
const pictureQuestion = computed(() => {
  return props.quizQuestion.questionType === "PICTURE_TITLE";
});
const processForm = () => {
  emits("answer", props.emptyAnswer);
};
</script>
