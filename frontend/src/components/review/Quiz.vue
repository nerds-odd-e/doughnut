<template>
  <BasicBreadcrumb :ancestors="quizQuestion.scope" />
  <div v-if="pictureQuestion">
    <ShowPicture :notePicture="sourceNote.notePicture" :pictureMask="sourceNote.noteAccessories.pictureMask" :opacity="1" />
  </div>
  <NoteFrameOfLinks v-bind="{ links: quizQuestion.hintLinks }">
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
  </NoteFrameOfLinks>

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
          <ShowPicture :notePicture="option.note.note.notePicture" :pictureMask="option.note.note.noteAccessories.pictureMask" :opacity="1" />
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

<script lang="ts">
import { defineComponent, PropType } from "vue";
import BasicBreadcrumb from "../commons/BasicBreadcrumb.vue";
import ShowPicture from "../notes/ShowPicture.vue";
import NoteFrameOfLinks from "../links/NoteFrameOfLinks.vue";
import TextInput from "../form/TextInput.vue";

export default defineComponent({
  props:{
    reviewPointViewedByUser: { type: Object as PropType<Generated.ReviewPointViewedByUser>, required: true},
    quizQuestion: { type: Object as PropType<Generated.QuizQuestionViewedByUser>, required: true},
    emptyAnswer: Object,
  },
  components: {
    BasicBreadcrumb, ShowPicture, NoteFrameOfLinks, TextInput
  },
  emits: ["answer"],
  computed: {
    sourceNote(): Generated.Note {
      if (!!this.reviewPointViewedByUser.noteWithPosition)
        return this.reviewPointViewedByUser.noteWithPosition.note.note;
      return this.reviewPointViewedByUser.linkViewedByUser!.sourceNoteWithPosition.note.note;
    },
    pictureQuestion() {
      return this.quizQuestion.questionType === "PICTURE_TITLE";
    },
  },
  methods: {
    processForm() {
      this.$emit("answer", this.emptyAnswer);
    }
  }
})
</script>
