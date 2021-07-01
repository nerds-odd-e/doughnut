<template>
<div>
    <div v-if="!!noteViewedByUser">
        <NoteViewedByUserWithoutChildren v-bind="{
            note: noteViewedByUser.note,
            links: noteViewedByUser.links,
            ancestors: noteViewedByUser.ancestors,
            notebook: noteViewedByUser.notebook,
            owns: noteViewedByUser.owns,
        }"/>
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
import NoteBreadcrumbForReview from "./NoteBreadcrumbForReview.vue"
import NoteViewedByUserWithoutChildren from "../notes/NoteViewedByUserWithoutChildren.vue"
import LinkShow from "../links/LinkShow.vue"

export default {
  name: "ShowReviewPoint",
  props: { noteViewedByUser: Object, linkViewedByUser: Object },
  components: {NoteBreadcrumbForReview, NoteViewedByUserWithoutChildren, LinkShow}
};
</script>
