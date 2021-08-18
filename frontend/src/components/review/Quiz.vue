<template>
    <NoteBreadcrumbForReview :ancestors="quizQuestion.scope"/>
    <div v-if="pictureQuestion">
        <ShowPicture :note="sourceNote.note" :opacity="1"/>
    </div>
    <LinkLists v-bind="{links: quizQuestion.hintLinks, owns: true, colors}">
        <div class="quiz-instruction">
            <pre style="white-space: pre-wrap;" v-if="!pictureQuestion" v-html="quizQuestion.description"/>
            <h2 v-if="!!quizQuestion.mainTopic" class="text-center">{{quizQuestion.mainTopic}}</h2>
        </div>

    </LinkLists>

    <div class="row mt-2" v-if="quizQuestion.questionType!=='SPELLING'">
        <div class="col-sm-6 mb-3" v-for="option in quizQuestion.options" :key="option.note.id">
            <form @submit.prevent.once="processForm">
                <button class="btn btn-secondary btn-lg btn-block" v-on:click="emptyAnswer.answerNoteId=option.note.id">
                    <div v-if="!option.picture">{{option.display}}</div>
                    <div v-else>
                        <ShowPicture :note="option.note" :opacity="1"/>
                    </div>
                </button>

            </form>
        </div>
    </div>

    <div v-else>
        <form @submit.prevent.once="processForm">
            <div class="aaa">
                <TextInput scopeName='review_point' field='answer' v-model="emptyAnswer.answer" placeholder='put your answer here' :autofocus="true"/>
            </div>
            <input type="submit" value="OK" class="btn btn-primary btn-lg btn-block"/>
        </form>
    </div>
</template>

<script>
  import NoteBreadcrumbForReview from "./NoteBreadcrumbForReview.vue"
  import ShowPicture from "../notes/ShowPicture.vue"
  import LinkLists from "../links/LinkLists.vue"
  import TextInput from "../form/TextInput.vue"

export default {
  name: "Quiz",
  props: {reviewPointViewedByUser: Object, quizQuestion: Object, emptyAnswer: Object, colors: Object},
  emits:['answer'],
  components: {NoteBreadcrumbForReview, ShowPicture, LinkLists, TextInput },
  computed: {
    sourceNote(){
        if (!!this.reviewPointViewedByUser.noteViewedByUser) return this.reviewPointViewedByUser.noteViewedByUser
        return this.reviewPointViewedByUser.linkViewedByUser.sourceNoteViewedByUser
    },
    pictureQuestion() {
        return this.quizQuestion.questionType === 'PICTURE_TITLE'
    }
  },
  methods: {
    processForm() {
      this.$emit('answer', this.emptyAnswer)
    }
  }
}

</script>
