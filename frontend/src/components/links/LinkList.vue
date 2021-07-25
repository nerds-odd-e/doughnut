<template>
    <ul>
        <template v-for="(linksOfType, linkType) in links" :key="linkType">
            <li v-if="linksOfType.reverse.length>0">
                <span>{{reverseLabel(linkType)}} </span>
                <span class="badge badge-warning ml-1 mr-1" v-for="link in linksOfType.reverse" :key="link.id">
                    <LinkWithHtmlLink :link="link" :note="link.sourceNote" :owns="owns"/>
                </span>
            </li>
            <li v-if="!!linksOfType.direct.length>0">
                <span>{{linkType}} </span>
                <span class="badge badge-light ml-1 mr-1" v-for="link in linksOfType.direct" :key="link.id">
                    <LinkWithHtmlLink :link="link" :note="link.targetNote" :owns="owns"/>
                </span>
            </li>
        </template>
    </ul>
</template>

<script>
  import LinkWithHtmlLink from "./LinkWithHtmlLink.vue"
  export default {
    name: "NoteShowWithTitle",
    props: { links: Object, owns: Boolean },
    components: { LinkWithHtmlLink },
    methods: {
        reverseLabel(lbl) {
            const {reversedLabel} = this.$staticInfo.linkTypeOptions.find(({label})=>lbl === label);
            return reversedLabel;
        }
    }
  }
</script>
