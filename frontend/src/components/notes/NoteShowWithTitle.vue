<template>
    <div>
        <slot></slot>
        <div v-if="!!note.noteContent.url">
            <label v-if="note.noteContent.urlIsVideo">Video Url:</label>
            <label v-else>Url:</label>
            <a :href="note.noteContent.url">{{note.noteContent.url}}</a>
        </div>
    </div>
    <ShowPicture :note="note" :opacity="0.2"/>
    <LinkList v-bind="{links, owns}"/>
</template>

<script>
  import ShowPicture from "./ShowPicture.vue"
  import LinkList from "../links/LinkList.vue"
  export default {
    name: "NoteShowWithTitle",
    props: { note: Object, links: Object, level: Number, owns: { type: Boolean, required: true } },
    components: { ShowPicture, LinkList }
  }
</script>
