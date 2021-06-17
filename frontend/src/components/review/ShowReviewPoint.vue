<template>
<div>
    <div v-if="!!reviewPoint.note">
        <!-- <NoteBreadcrumbForReview :ancestors="reviewPoint.sourceNoteAncestors" /> -->
        <div class="jumbotron py-4 mb-2">
            <NoteShow v-bind="{...sourceNoteViewedByUser, level: 1, forBazaar: false}"/>
        </div>
    </div>

    <!-- <div v-if="!!reviewPoint.link"> -->
    <div v-if="false">
        <div class="jumbotron py-4 mb-2">
            <div th:replace="_fragments/link_fragments :: linkShow(*{link}, ~{:: .link-content})">
                <span class="link-content badge badge-light mr-1" th:text="*{type}"/>
            </div>
        </div>
    </div>
</div>
</template>

<script setup>
  import NoteBreadcrumbForReview from "./NoteBreadcrumbForReview.vue"
  import NoteShow from "../notes/NoteShow.vue"
  const props = defineProps({reviewPoint: Object, sourceNoteViewedByUser: Object})
</script>
