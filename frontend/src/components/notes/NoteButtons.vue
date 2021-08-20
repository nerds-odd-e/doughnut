<template>
<div class="btn-group btn-group-sm">

    <NoteNewButton :parentId="note.id">
        <template #default="{open}">
            <button class="btn btn-small" @click="open()" :title="`Add Child Note`">
                <SvgAddChild/>
            </button>
        </template>
    </NoteNewButton>

    <NoteNewButton :parentId="note.parentId" v-if="!!note.parentId && addSibling">
        <template #default="{open}">
            <button class="btn btn-small" @click="open()" title="Add Sibling Note">
                <SvgAddSibling/>
            </button>
        </template>
    </NoteNewButton>

    <NoteEditButton :noteId="note.id" :oldTitle="note.title" @updated="$emit('updated')"/>
    <LinkNoteButton :note="note" @updated="$emit('updated')"/>
    <a class="btn btn-light dropdown-toggle"
            data-bs-toggle="dropdown" aria-haspopup="true"
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
  import SvgAddChild from "../svgs/SvgAddChild.vue"
  import SvgAddSibling from "../svgs/SvgAddSibling.vue"
  import SvgCog from "../svgs/SvgCog.vue"
  import SvgRemove from "../svgs/SvgRemove.vue"
  import LinkNoteButton from "../links/LinkNoteButton.vue"
  import ReviewSettingEditButton from "../review/ReviewSettingEditButton.vue"
  import NoteEditButton from "./NoteEditButton.vue"
  import NoteNewButton from "./NoteNewButton.vue"
  import { restPost } from '../../restful/restful'
  export default {
    name: 'NoteButtons',
    props: {note: Object, addSibling: Boolean},
    emits: ['updated'],
    components: { SvgCog, SvgAddChild, SvgAddSibling, ReviewSettingEditButton, SvgRemove, LinkNoteButton, NoteEditButton, NoteNewButton },

    methods: {
        async deleteNote() {
            if(await this.$popups.confirm(`Are you sure to delete this note?`)) {
                restPost(`/api/notes/${this.note.id}/delete`, {}, r=>{})
                  .then( r=>{
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
