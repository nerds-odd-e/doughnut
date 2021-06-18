<template>
        <NoteBreadcrumbForReview v-bind="sourceNote"/>
        <div v-if="quizQuestion.pictureQuestion">
            <ShowPicture :note="sourceNote.note" :opacity="1"/>
        </div>
        <div class="quiz-instruction">
          <pre style="white-space: pre-wrap;" v-if="!quizQuestion.pictureQuestion" v-html="quizQuestion.description"/>
          <h2 v-if="!!quizQuestion.mainTopic" class="text-center">{{quizQuestion.mainTopic}}</h2>
        </div>

        <div class="row mt-2" th:unless="${quizQuestion.getQuestionType().label=='spelling'}">
            <div class="col-sm-6 mb-3" th:each="option:${quizQuestion.getOptions()}">
                <form th:action="@{/reviews/{id}/answer(id=${reviewPoint.id})}" th:object="${emptyAnswer}" method="post">
                    <input type="hidden" name="answerNote" th:value="${option.note.id}"/>
                    <input type="hidden" th:field="*{questionType}"/>
                    <button class="btn btn-secondary btn-lg btn-block">
                        <div th:text="${option.display}" th:unless="${option.isPicture}"/>
                        <div th:if="${option.isPicture}">
                            <div th:replace="_fragments/note_fragments :: showPicture(${option.note}, 1)"/>
                        </div>
                    </button>

                </form>
            </div>
        </div>

        <div th:if="${quizQuestion.getQuestionType().label=='spelling'}">
            <form th:action="@{/reviews/{id}/answer(id=${reviewPoint.id})}" th:object="${emptyAnswer}" method="post">
                <input type="hidden" th:field="*{questionType}"/>
                <div class="aaa" th:insert="_fragments/forms :: textInput('review_point', 'answer', 'put your answer here', true)"/>
                <input type="submit" value="OK" class="btn btn-primary btn-lg btn-block"/>
            </form>
        </div>
</template>

<script setup>
  import NoteBreadcrumbForReview from "./NoteBreadcrumbForReview.vue"
  import ShowPicture from "../notes/ShowPicture.vue"
  import { computed } from 'vue'

  const props = defineProps({reviewPointViewedByUser: Object, quizQuestion: Object})
  const sourceNote = computed(()=>{
    if (!!props.reviewPointViewedByUser.noteViewedByUser) return props.reviewPointViewedByUser.noteViewedByUser
    return props.reviewPointViewedByUser.linkViewedByUser.sourceNoteViewedByUser
  })
</script>
