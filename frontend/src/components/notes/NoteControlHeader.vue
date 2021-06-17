<template>
<div >
    <NoteOwnerBreadcrumb :ancestors="ancestors" :notebook="notebook">
        <li class="breadcrumb-item">{{note.title}}</li>
        <li class="breadcrumb-item">
            <a class="text-secondary" :href="`/notes/${note.id}/new`">{{`(Add ${note.noteTypeDisplay})`}}</a>
        </li>
    </NoteOwnerBreadcrumb>
    <div v-if="!note.head">
        <SvgDownRight/>
        <a :href="`/notes/${ancestors[ancestors.length -1].id}/new`">Add Sibling Note</a>
    </div>
</div>
</template>

<script setup>
  import SvgEdit from "../svgs/SvgEdit.vue"
  import SvgArticle from "../svgs/SvgArticle.vue"
  import SvgLinkNote from "../svgs/SvgLinkNote.vue"
  import SvgCog from "../svgs/SvgCog.vue"
  import SvgDownRight from "../svgs/SvgDownRight.vue"
  import SvgReviewSetting from "../svgs/SvgReviewSetting.vue"
  import NoteOwnerBreadcrumb from "./NoteOwnerBreadcrumb.vue"
  const props = defineProps({note: Object, ancestors: Array, notebook: Object})
</script>
