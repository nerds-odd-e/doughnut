<template>
    <ul>
        <template v-for="(linksOfType, linkType) in links" :key="linkType">
            <li v-if="linksOfType.reverse.length>0">
                <span>{{reverseLabel(linkType)}} </span>
                <LinkLink  v-for="link in linksOfType.reverse" :key="link.id" :link="link" :reverse="true" :owns="owns"/>
            </li>
        </template>
    </ul>
</template>

<script>
  import LinkLink from "./LinkLink.vue"
  export default {
    name: "NoteShowWithTitle",
    props: { links: Object, owns: Boolean },
    components: { LinkLink },
    methods: {
        reverseLabel(lbl) {
            if(!this.$staticInfo) {
                 return
            }
            const {reversedLabel} = this.$staticInfo.linkTypeOptions.find(({label})=>lbl === label);
            return reversedLabel;
        }
    }
  }
</script>
