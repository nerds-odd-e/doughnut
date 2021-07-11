<template>
<div class="btn-group btn-group-sm">
    <NoteEditButton :noteId="note.id" :oldTitle="note.title" @updated="$emit('updated')"/>
    <a role="button" class="btn btn-sm" title="Cards View" :href="`/notes/articles/${note.id}`">
        <SvgArticle/>
    </a>
    <LinkNoteButton :note="note" @updated="$emit('updated')"/>
    <a class="btn btn-light dropdown-toggle"
            data-toggle="dropdown" aria-haspopup="true"
            aria-expanded="false" role="button" title="more options">
        <SvgCog/>
    </a>
    <div class="dropdown-menu dropdown-menu-right">
        <ReviewSettingEditButton :noteId="note.id" :oldTitle="note.title" @updated="$emit('updated')">
            Edit review settings
        </ReviewSettingEditButton>
        <button class="dropdown-item" title="delete note" v-on:click="deleteNote()">
          <SvgRemove/>
          Delete
        </button>
    </div>
</div>
</template>

<script>
  import SvgArticle from "../svgs/SvgArticle.vue"
  import SvgCog from "../svgs/SvgCog.vue"
  import SvgRemove from "../svgs/SvgRemove.vue"
  import LinkNoteButton from "../links/LinkNoteButton.vue"
  import ReviewSettingEditButton from "../review/ReviewSettingEditButton.vue"
  import NoteEditButton from "./NoteEditButton.vue"
  import { restPost } from '../../restful/restful'
  export default {
    name: 'NoteButtons',
    props: {note: Object},
    emits: ['updated'],
    components: { SvgArticle, SvgCog, ReviewSettingEditButton, SvgRemove, LinkNoteButton, NoteEditButton },
    methods: {
        async deleteNote() {
            if(await this.$popups.confirm(`Are you sure to delete this note?`)) {
                restPost(`/api/notes/${this.note.id}/delete`, {}, r=>{}, r=>{
                  if (!!r.noteId) {
                      this.$router.push({name: 'noteShow', params: {noteId: r.noteId}})
                  }
                  else {
                      this.$router.push({name: 'notebooks'})
                  }
                  this.$emit('updated')
                })
            }
        }
    }
  }
</script>
