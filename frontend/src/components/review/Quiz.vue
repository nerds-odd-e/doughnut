<template>
        <NoteBreadcrumbForReview v-bind="sourceNote"/>
        <div v-if="quizQuestion.pictureQuestion">
            <ShowPicture :note="sourceNote.note" :opacity="1"/>
        </div>
        <div class="quiz-instruction">
          <pre style="white-space: pre-wrap;" v-if="!quizQuestion.pictureQuestion" v-html="quizQuestion.description"/>
          <h2 v-if="!!quizQuestion.mainTopic" class="text-center">{{quizQuestion.mainTopic}}</h2>
        </div>

        <div class="row mt-2" v-if="quizQuestion.questionType!=='SPELLING'">
            <div class="col-sm-6 mb-3" v-for="option in quizQuestion.options" :key="option.note.id">
                <form :action="`/reviews/${reviewPointViewedByUser.reviewPoint.id}/answer`" method="post">
                    <input type="hidden" name="answerNote" :value="option.note.id"/>
                    <input type="hidden" name="questionType" :value="emptyAnswer.questionType"/>
                    <button class="btn btn-secondary btn-lg btn-block">
                        <div v-if="!option.picture">{{option.display}}</div>
                        <div v-else>
                            <ShowPicture :note="option.note" :opacity="1"/>
                        </div>
                    </button>

                </form>
            </div>
        </div>

        <div v-else>
            <form :action="`/reviews/${reviewPointViewedByUser.reviewPoint.id}/answer`" method="post">
                <input type="hidden" name="questionType" :value="emptyAnswer.questionType"/>
                <div class="aaa">
                   <TextInput scopeName='review_point' field='answer' placeholder='put your answer here' :autofocus="true"/>
                </div>
                <input type="submit" value="OK" class="btn btn-primary btn-lg btn-block"/>
            </form>
        </div>
</template>

<script setup>
  import NoteBreadcrumbForReview from "./NoteBreadcrumbForReview.vue"
  import ShowPicture from "../notes/ShowPicture.vue"
  import TextInput from "../form/TextInput.vue"
  import { computed } from 'vue'

  const props = defineProps({reviewPointViewedByUser: Object, quizQuestion: Object, emptyAnswer: Object})
  const sourceNote = computed(()=>{
    if (!!props.reviewPointViewedByUser.noteViewedByUser) return props.reviewPointViewedByUser.noteViewedByUser
    return props.reviewPointViewedByUser.linkViewedByUser.sourceNoteViewedByUser
  })
</script>
