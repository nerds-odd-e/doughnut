<template>
<div>
    <div v-if="!!noteViewedByUser">
        <NoteViewedByUserWithoutChildren v-bind="{
            note: noteViewedByUser.note,
            links: noteViewedByUser.links,
            ancestors: noteViewedByUser.ancestors,
            notebook: noteViewedByUser.notebook,
            owns: noteViewedByUser.owns,
        }" @updated="$emit('updated')"/>
    </div>

    <div v-if="!!linkViewedByUser">
        <div class="jumbotron py-4 mb-2">
            <LinkShow v-bind="linkViewedByUser">
                <LinkNob v-bind="{link: linkViewedByUser}" :owns="true"/>
                <span class="badge bg-light text-dark"> {{linkViewedByUser.linkTypeLabel}}</span>
            </LinkShow>
        </div>
    </div>
</div>
</template>

<script setup>
import NoteBreadcrumbForReview from "./NoteBreadcrumbForReview.vue"
import NoteViewedByUserWithoutChildren from "../notes/NoteViewedByUserWithoutChildren.vue"
import LinkShow from "../links/LinkShow.vue"
import LinkNob from "../links/LinkNob.vue"
  const props = defineProps({ noteViewedByUser: Object, linkViewedByUser: Object })
  const emits = defineEmits(['updated'])
</script>
