<template>
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
  import LinkWithHtmlLink from "./LinkWithHtmlLink.vue"
  export default {
    name: "NoteShowWithTitle",
    props: { links: Object, owns: Boolean },
    components: { LinkWithHtmlLink }
  }
</script>
