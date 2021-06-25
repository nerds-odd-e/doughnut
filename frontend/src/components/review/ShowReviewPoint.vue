<template>
<div>
    <div v-if="!!noteViewedByUser">
        <NoteBreadcrumbForReview :ancestors="noteViewedByUser.ancestors" />
        <div class="jumbotron py-4 mb-2">
            <NoteShow v-bind="{note: noteViewedByUser.note, links: noteViewedByUser.links, level: 1}"/>
        </div>
    </div>

    <div v-if="!!linkViewedByUser">
        <div class="jumbotron py-4 mb-2">
            <LinkShow v-bind="linkViewedByUser">
                <span class="badge badge-light mr-1"> {{linkViewedByUser.linkTypeLabel}}</span>
            </LinkShow>
        </div>
    </div>
</div>
</template>

<script>
export default { name: "ShowReviewPoint" };
</script>

<script setup>
  import NoteBreadcrumbForReview from "./NoteBreadcrumbForReview.vue"
  import NoteShow from "../notes/NoteShow.vue"
  import LinkShow from "../links/LinkShow.vue"
  const props = defineProps({ noteViewedByUser: Object, linkViewedByUser: Object })
</script>
