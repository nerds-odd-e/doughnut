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
                <form @submit.prevent="processForm">
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
            <form @submit.prevent="processForm">
                <div class="aaa">
                   <TextInput scopeName='review_point' field='answer' v-model="emptyAnswer.answer" placeholder='put your answer here' :autofocus="true"/>
                </div>
                <input type="submit" value="OK" class="btn btn-primary btn-lg btn-block"/>
            </form>
        </div>
</template>

<script setup>
  import NoteBreadcrumbForReview from "./NoteBreadcrumbForReview.vue"
  import ShowPicture from "../notes/ShowPicture.vue"
  import TextInput from "../form/TextInput.vue"
  import { computed, defineEmit, ref } from 'vue'

  const props = defineProps({reviewPointViewedByUser: Object, quizQuestion: Object, emptyAnswer: Object})
  const emit = defineEmit(['answered'])
  const sourceNote = computed(()=>{
    if (!!props.reviewPointViewedByUser.noteViewedByUser) return props.reviewPointViewedByUser.noteViewedByUser
    return props.reviewPointViewedByUser.linkViewedByUser.sourceNoteViewedByUser
  })

  const processForm = function() {
          console.log(props.emptyAnswer);
      fetch(`/api/reviews/${props.reviewPointViewedByUser.reviewPoint.id}/answer`, {
        method: 'POST',
        headers: {
          'Accept': 'application/json',
          'Content-Type': 'application/json'
        },
        body: JSON.stringify(props.emptyAnswer)
      })
        .then(res => {
          return res.json();
        })
        .then(resp => {
          emit('answered', resp)
        })
        .catch(error => {
          window.alert(error);
        });
    }

</script>
