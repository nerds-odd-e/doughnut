<template>
  <LoadingPage v-bind="{loading, contentExists: !!linkViewedByUser}">
    <div v-if="linkViewedByUser">
        <LinkShow v-bind="linkViewedByUser">
            <div class="link-content">
              <div>
                <Select v-if="!!staticInfo" scopeName='link' field='linkType' v-model="formData.typeId" :options="staticInfo.linkTypeOptions"/>
                <button class="btn btn-primary" v-on:click="updateLink()">Update</button>
                <button class="btn btn-danger" v-on:click="deleteLink()">Delete</button>
              </div>
              <nav class="nav d-flex flex-row-reverse p-0">
                <NoteStatisticsButton :linkid="linkViewedByUser.id"/>
              </nav>
            </div>
        </LinkShow>
    </div>
  </LoadingPage>
</template>

<script setup>
import LinkShow from "../components/links/LinkShow.vue"
import Select from "../components/form/Select.vue"
import NoteStatisticsButton from '../components/notes/NoteStatisticsButton.vue'
import LoadingPage from "./LoadingPage.vue"
import {restGet, restPost } from "../restful/restful"
import { computed, ref, watch, defineProps } from "vue"

const props = defineProps({linkid: Number, staticInfo: Array})
const emit = defineEmit(['redirect'])
const linkViewedByUser = ref(null)
const loading = ref(false)

const formData = computed(()=>!linkViewedByUser.value ? null : {typeId: linkViewedByUser.value.linkTypeId})

const fetchData = async () => {
  restGet(`/api/links/${props.linkid}`, loading, (res) => linkViewedByUser.value = res)
}

const updateLink = async () => {
  restPost(`/api/links/${props.linkid}`, formData.value, loading, (res) => emit("redirect", {name: "noteShow", params: { noteid: res.noteId}}))
}

const deleteLink = async () => {
  if(!confirm('Are you sure to delete this link?')) return;
  restPost(`/api/links/${props.linkid}/delete`, null, loading, (res) => emit("redirect", {name: "noteShow", replace: true, params: { noteid: res.noteId}}))
}

watch(()=>props.linkid, ()=>fetchData())
fetchData()

</script>
