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
                <template v-if="!forBazaar">
                    <LinkWithHtmlLink :link="link" :note="link.targetNote"/>
                </template>
                <a v-else :href="`/bazaar/notes/${link.targetNote.id}`">{{link.targetNote.title}}</a>
            </span>
            <span class="badge badge-warning ml-1 mr-1" v-for="link in linksOfType.reverse" :key="link.id">
                <template v-if="!forBazaar">
                    <LinkWithHtmlLink :link="link" :note="link.sourceNote"/>
                </template>
                <a v-else :href="`/bazaar/notes/${link.sourceNote.id}`">{{link.sourceNote.title}}</a>
            </span>
        </li>
    </ul>
</template>

<script setup>
  import ShowPicture from "./ShowPicture.vue"
  import LinkWithHtmlLink from "./LinkWithHtmlLink.vue"
  const props = defineProps({note: Object, links: Object, level: Number, forBazaar: Boolean})
</script>
