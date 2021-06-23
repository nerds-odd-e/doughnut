<template>
  <LoadingPage v-bind="{loading, contentExists: !!linkViewedByUser}">
    <div v-if="linkViewedByUser">
        <LinkShow v-bind="linkViewedByUser">
            <div class="link-content">
                <form :action="`/links/${linkViewedByUser.id}`" method="post">
                    <Select v-if="!!staticInfo" scopeName='link' field='linkType' v-model="linkViewedByUser.linkTypeId" :options="staticInfo.linkTypeOptions"/>
                    <input type="submit" name="submit" value="Update" class="btn btn-primary"/>
                    <input type="submit" name="delete" value="Delete" class="btn btn-danger"
                           onclick="return confirm('Are you sure to delete this link?')"/>
                </form>
                <nav class="nav d-flex flex-row-reverse p-0">
                    <div id="partials" th:data-linkid="${link.id}"> </div>
                </nav>
            </div>
        </LinkShow>
    </div>
  </LoadingPage>
</template>

<script setup>
import LinkShow from "../components/links/LinkShow.vue"
import Select from "../components/form/Select.vue"
import LoadingPage from "./LoadingPage.vue"
import {restGet} from "../restful/restful"
import { ref, watch, defineProps } from "vue"

const props = defineProps({linkid: Number, staticInfo: Array})
const linkViewedByUser = ref(null)
const loading = ref(false)

const fetchData = async () => {
  restGet(`/api/links/${props.linkid}`, loading, (res) => linkViewedByUser.value = res)
}

watch(()=>props.linkid, ()=>fetchData())
fetchData()

</script>
