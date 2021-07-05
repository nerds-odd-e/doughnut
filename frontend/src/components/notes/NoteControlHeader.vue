<template>
    <div>
        <NoteOwnerBreadcrumb :ancestors="ancestors" :notebook="notebook">
            <li class="breadcrumb-item">{{note.title}}</li>
            <li class="breadcrumb-item">
                <NoteNewButton :notebook="notebook" :ancestors="[...ancestors, note]">
                    <template #default="{open}">
                        <a class="text-secondary" @click="open()">{{`(Add ${note.noteTypeDisplay})`}}</a>
                    </template>
                </NoteNewButton>
            </li>
        </NoteOwnerBreadcrumb>
        <div v-if="!note.head">
            <NoteNewButton :notebook="notebook" :ancestors="ancestors">
                <template #default="{open}">
                    <SvgDownRight/>
                    <a class="text-secondary" @click="open()">Add Sibling Note</a>
                </template>
            </NoteNewButton>
        </div>
    </div>
</template>

<script>
  import SvgEdit from "../svgs/SvgEdit.vue"
  import SvgArticle from "../svgs/SvgArticle.vue"
  import SvgLinkNote from "../svgs/SvgLinkNote.vue"
  import SvgCog from "../svgs/SvgCog.vue"
  import SvgDownRight from "../svgs/SvgDownRight.vue"
  import SvgReviewSetting from "../svgs/SvgReviewSetting.vue"
  import NoteOwnerBreadcrumb from "./NoteOwnerBreadcrumb.vue"
  import NoteNewButton from "./NoteNewButton.vue"
  export default {
    props: {note: Object, ancestors: Array, notebook: Object},
    components: { SvgEdit, SvgArticle, SvgLinkNote, SvgCog, SvgDownRight, SvgReviewSetting, NoteOwnerBreadcrumb, NoteNewButton }
  }
</script>
