<template>
   <div th:fragment="header">
        <nav class="navbar navbar-expand-lg navbar-dark bg-dark">
            <button class="navbar-toggler" type="button" data-toggle="collapse" data-target="#navbarSupportedContent" aria-controls="navbarSupportedContent" aria-expanded="false" aria-label="Toggle navigation">
                <span class="navbar-toggler-icon"></span>
            </button>
            <a class="navbar-brand" href="/#">
                <img src="/odd-e.png" width="30" height="30" class="d-inline-block align-top" alt="">
                Doughnut
            </a>
            <div class="collapse navbar-collapse" id="navbarSupportedContent">
                <ul class="navbar-nav mr-auto">
                    <li class="nav-item active ">
                        <router-link class="nav-link" :to="{name: 'notebooks'}">My Notes</router-link>
                    </li>
                    <li class="nav-item active">
                        <router-link class="nav-link" :to="{name: 'reviews'}">Review</router-link>
                    </li>
                    <li class="nav-item active">
                        <a class="nav-link" href="/circles">Circles</a>
                    </li>
                    <li class="nav-item active">
                        <a class="nav-link" href="/bazaar">Bazaar</a>
                    </li>
                    <li class="nav-item active" v-if="user && user.isDeveloper">
                        <a class="nav-link" href="/failure-report-list">Failure Reports</a>
                    </li>
                </ul>
                <template v-if="user">
                  <span style="margin-right:10px;" class="navbar-text">
                      <a href="/users/edit">{{user.name}}</a>
                  </span>
                  <form class="form-inline my-2 my-lg-0" th:action="@{/logout}" method="post">
                      <input class= "btn btn-outline-success my-2 my-sm-0" type= "submit" value="Logout">
                  </form>
                </template>
                <a v-else class= "btn btn-outline-primary my-2 my-sm-0" href="/login">Login via Github</a>
            </div>
        </nav>
    </div>
</template>

<script>
export default {
  props: {user: Object}
}
</script>