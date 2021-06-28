<template>
<div >
    <NoteOwnerBreadcrumb :ancestors="ancestors" :notebook="notebook">
        <li class="breadcrumb-item">{{note.title}}</li>
        <li class="breadcrumb-item">
            <RelativeRouterLink class="text-secondary" :to="{name: 'noteNew', params: {noteid: note.id}}">{{`(Add ${note.noteTypeDisplay})`}}</RelativeRouterLink>
        </li>
    </NoteOwnerBreadcrumb>
    <div v-if="!note.head">
        <SvgDownRight/>
        <RelativeRouterLink :to="{name: 'noteNew', params: {noteid: ancestors[ancestors.length -1].id}}">Add Sibling Note</RelativeRouterLink>
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
  import RelativeRouterLink from "../../routes/RelativeRouterLink.vue"
  const props = defineProps({note: Object, ancestors: Array, notebook: Object})
</script>
