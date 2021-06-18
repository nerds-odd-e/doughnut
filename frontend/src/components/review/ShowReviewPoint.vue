<template>
<div>
    <div v-if="!!reviewPoint.note">
        <!-- <NoteBreadcrumbForReview :ancestors="reviewPoint.sourceNoteAncestors" /> -->
        <div class="jumbotron py-4 mb-2">
            <NoteShow v-bind="{...sourceNoteViewedByUser, level: 1, forBazaar: false}"/>
        </div>
    </div>

    <div v-if="!!reviewPoint.link">
        <div class="jumbotron py-4 mb-2">
            <LinkShow v-bind="linkViewedByUser">
                <span class="badge badge-light mr-1"> {{link.linkTypeLabel}}</span>
            </LinkShow>
        </div>
    </div>
</div>
</template>

<script setup>
  import NoteBreadcrumbForReview from "./NoteBreadcrumbForReview.vue"
  import NoteShow from "../notes/NoteShow.vue"
  const props = defineProps({reviewPoint: Object, sourceNoteViewedByUser: Object, linkViewedByUser: Object })
</script>
