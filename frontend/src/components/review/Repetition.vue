<template>
        <div class="btn-toolbar justify-content-between">
            <form :action="`/reviews/${reviewPoint.id}`" method="post">
                <div class="btn-group" role="group" aria-label="First group">
                    <button type="submit" class="btn btn-light" id="repeat-again" name="again"
                            title="repeat immediately">
                        <SvgFailed/>
                    </button>
                    <template v-if="!sadOnly">
                      <button type="submit" class="btn btn-light" id="repeat-sad" name="sad"
                              title="reduce next repeat interval (days) by half">
                          <SvgSad/>
                      </button>
                      <button type="submit" class="btn btn-light" id="repeat-satisfied" name="satisfying"
                              title="use normal repeat interval (days)">
                          <SvgSatisfying/>
                      </button>
                      <button type="submit" class="btn btn-light" id="repeat-happy" name="happy"
                              title="add to next repeat interval (days) by half">
                          <SvgHappy/>
                      </button>
                    </template>

                </div>
            </form>
            <div class="btn-group dropup">
                <button type="button" id="more-action-for-repeat" class="btn btn-light dropdown-toggle"
                        data-toggle="dropdown" aria-haspopup="true"
                        aria-expanded="false">
                    <svg th:replace="_fragments/svgs :: cog"/>
                </button>
                <div class="dropdown-menu dropdown-menu-right">
                    <a class="dropdown-item"
                       th:href="@{/notes/{id}/review_setting(id=${reviewPoint.getSourceNote().id})}">
                        <svg th:replace="_fragments/svgs :: reviewSetting"/>
                        Edit Review Settings
                    </a>
                    <a class="dropdown-item" th:href="@{/links/{id}/link(id=${reviewPoint.getSourceNote().id})}">
                        <svg th:replace="_fragments/svgs :: linkNote"/>
                        Make Link
                        </a>
                        <a class="dropdown-item"
                           th:href="@{/notes/{id}/edit(id=${reviewPoint.getSourceNote().id})}">
                            <svg th:replace="_fragments/svgs :: edit"/>
                            Edit Note
                        </a>
                        <div class="dropdown-divider"></div>
                        <form th:action="@{/reviews/{id}(id=${reviewPoint.id})}" th:object="${reviewPoint}"
                              method="post"
                              onsubmit="return confirm('Are you sure to hide this note from reviewing in the future?')">
                            <button type="submit" class="dropdown-item" name="remove">
                                <svg th:replace="_fragments/svgs :: noReview"/>
                                Remove This Note from Review
                            </button>
                        </form>
                </div>
            </div>
        </div>
</template>

<script setup>
  import SvgSad from "../svgs/SvgSad.vue"
  import SvgSatisfying from "../svgs/SvgSatisfying.vue"
  import SvgFailed from "../svgs/SvgFailed.vue"
  import SvgHappy from "../svgs/SvgHappy.vue"
  const props = defineProps({reviewPoint: Object, sadOnly: Boolean})
</script>
