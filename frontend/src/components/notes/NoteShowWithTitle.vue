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
    <ul>
        <li v-for="(linksOfType, linkType) in links" :key="linkType">
            <span>{{linkType}} </span>
            <span class="badge badge-light ml-1 mr-1" v-for="link in linksOfType.direct" :key="link.id">
                <LinkWithHtmlLink :link="link" :note="link.targetNote" :owns="owns"/>
            </span>
            <span class="badge badge-warning ml-1 mr-1" v-for="link in linksOfType.reverse" :key="link.id">
                <LinkWithHtmlLink :link="link" :note="link.sourceNote" :owns="owns"/>
            </span>
        </li>
    </ul>
</template>

<script>
export default { name: "NoteShowWithTitle" };
</script>

<script setup>
  import ShowPicture from "./ShowPicture.vue"
  import LinkWithHtmlLink from "../links/LinkWithHtmlLink.vue"
  const props = defineProps({note: Object, links: Object, level: Number, owns: Boolean})
</script>
