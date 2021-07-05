<template>
<div class="btn-group btn-group-sm">
    <NoteEditButton :note="note" @updated="$emit('updated')"/>
    <a role="button" class="btn btn-sm" title="Cards View" :href="`/notes/articles/${note.id}`">
        <SvgArticle/>
    </a>
    <LinkNoteButton :noteTitle="note.title" :noteId="note.id" @updated="$emit('updated')"/>
    <a class="btn btn-light dropdown-toggle"
            data-toggle="dropdown" aria-haspopup="true"
            aria-expanded="false" role="button" title="more options">
        <SvgCog/>
    </a>
    <div class="dropdown-menu dropdown-menu-right">
        <a  class="dropdown-item" :href="`/notes/${note.id}/review_setting`" title="edit review settings">
            <SvgReviewSetting/>
            Edit review settings
        </a>
        <form :action="`/notes/${note.id}/delete`" method="post"
                      onsubmit="return confirm('Are you sure to delete?')">
            <button class="dropdown-item" title="delete note">
                <SvgRemove/>
                Delete
            </button>
        </form>
    </div>
</div>
</template>

<script>
  import SvgArticle from "../svgs/SvgArticle.vue"
  import SvgCog from "../svgs/SvgCog.vue"
  import SvgReviewSetting from "../svgs/SvgReviewSetting.vue"
  import SvgRemove from "../svgs/SvgRemove.vue"
  import LinkNoteButton from "../links/LinkNoteButton.vue"
  import NoteEditButton from "./NoteEditButton.vue"
  export default {
    name: 'NoteButtons',
    props: {note: Object},
    emits: ['updated'],
    components: { SvgArticle, SvgCog, SvgReviewSetting, SvgReviewSetting, SvgRemove, LinkNoteButton, NoteEditButton }
  }
</script>
