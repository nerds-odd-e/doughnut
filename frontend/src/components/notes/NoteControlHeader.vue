<template>
    <div>
        <NoteOwnerBreadcrumb :ancestors="ancestors" :notebook="notebook">
            <li class="breadcrumb-item">{{note.title}}</li>
            <li class="breadcrumb-item">
                <NoteNewButton :parentId="note.id">
                    <template #default="{open}">
                        <a class="text-secondary" @click="open()">{{`(Add ${note.noteTypeDisplay})`}}</a>
                    </template>
                </NoteNewButton>
            </li>
        </NoteOwnerBreadcrumb>
        <div v-if="!note.head">
            <NoteNewButton :parentId="ancestors[ancestors.length - 1].id">
                <template #default="{open}">
                    <SvgDownRight/>
                    <a class="text-secondary" @click="open()">Add Sibling Note</a>
                </template>
            </NoteNewButton>
        </div>
    </div>
</template>

<script>
  import SvgDownRight from "../svgs/SvgAddSibling.vue"
  import NoteOwnerBreadcrumb from "./NoteOwnerBreadcrumb.vue"
  import NoteNewButton from "./NoteNewButton.vue"
  export default {
    props: {note: Object, ancestors: Array, notebook: Object},
    components: { SvgDownRight, NoteOwnerBreadcrumb, NoteNewButton }
  }
</script>
