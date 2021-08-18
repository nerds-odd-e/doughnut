<template>
    <ul class="parent-links">
        <template v-for="(linksOfType, linkType) in parentLinks" :key="linkType">
            <li  v-for="link in linksOfType.direct" :key="link.id">
                <LinkLink  v-bind="{link, owns, colors: staticInfo.colors}" :reverse="false"/>
            </li>
        </template>
        <li v-for="(linksOfType, linkType) in tagLinks" :key="linkType">
            <LinkLink v-for="link in linksOfType.direct" :key="link.id" v-bind="{link, owns, colors: staticInfo.colors}" :reverse="false"/>
        </li>
    </ul>
    <slot />

    <ul>
        <template v-for="(linksOfType, linkType) in links" :key="linkType">
            <li v-if="linksOfType.reverse.length>0">
                <span>{{reverseLabel(linkType)}} </span>
                <LinkLink  v-for="link in linksOfType.reverse" :key="link.id" v-bind="{link, owns, colors: staticInfo.colors}" :reverse="true"/>
            </li>
        </template>
    </ul>

</template>

<script setup>
  import { computed } from "@vue/runtime-core";
  import LinkLink from "./LinkLink.vue"


  const props = defineProps( { links: Object, owns: Boolean, staticInfo: Object } )
  const reverseLabel = function (lbl) {
            if(!props.staticInfo || !props.staticInfo.linkTypeOptions) {
                 return
            }
            const {reversedLabel} = props.staticInfo.linkTypeOptions.find(({label})=>lbl === label);
            return reversedLabel;
        }

  const taggingTypes = ()=>{
      if(!props.staticInfo || !props.staticInfo.linkTypeOptions) return []
      return props.staticInfo.linkTypeOptions.filter(t=>t.value==8).map(t=>t.label)
  }

  const parentLinks = computed(()=>{
      const tTypes = taggingTypes()
      if(!props.links) return
      return Object.fromEntries(Object.entries(props.links).filter(t=>!tTypes.includes(t[0])))
  })

  const tagLinks = computed(()=>{
      const tTypes = taggingTypes()
      if(!props.links) return
      return Object.fromEntries(Object.entries(props.links).filter(t=>tTypes.includes(t[0])))
  })

</script>

<style scoped>
.parent-links {
    list-style: none;
    padding: 0px;
    margin: 0px;
}
.parent-links li {
    width: 100%;
    border-radius: 10px;
    border-top: solid 1px black;
}
</style>
