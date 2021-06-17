<template>
  <div
    class="font-sans antialiased col-lg-9"
    id="article-container"
    style="width:100%;padding-left:50px; margin-bottom:50px;"
  >
    <div
      v-if="articleList.length"
      v-for="article in articleList"
      class="row article"
    >
      <div
        class="row title"
        style="width:100%;padding:10px;font-size:24px;font-weight:bold;"
      >
        {{ article.title }}
      </div>
      <div class="row authorName" style="width:100%;padding:10px">
        {{ article.author }}
      </div>
      <div class="row createdAt" style="width:100%;padding:10px">
        {{ article.createdDatetime }}
      </div>
      <div class="row content" style="width:100%;padding:10px">
        {{ article.description }}
      </div>
      <div style="width:100%; height: 30px;"></div>
    </div>
  </div>
</template>

<script>
export default {
  name: "Blog",
  data() {
    return {
      blogInfo: [],
      articleList: [],
      apiUrl: "/api/blog/posts_by_website_name/odd-e-blog"
    };
  },

  mounted() {
    this.fetchBlogPosts();
  },

  methods: {
    fetchBlogPosts() {
      fetch(this.apiUrl)
        .then(res => {
        return res.json();
        })
        .then(articles => {
          this.articleList = articles;
        })
        .catch(error => {
          alert(error);
        });
    }
  }
};
</script>
