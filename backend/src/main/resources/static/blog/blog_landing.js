const app = Vue.createApp({
  el: "#app",
  data() {
    return {
      blogInfo: [],
      yearList: [],
      articleList: [],
      apiUrl: "../api/blog/posts_by_website_name/odd-e-blog",
      apiYearsUrl: "../api/blog/year_list"
    };
  },
  mounted() {
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

    fetch(this.apiYearsUrl)
      .then(res => {
        return res.json();
      })
      .then(years => {
        this.yearList = years;
      })
      .catch(error => {
        alert(error);
      });
  }
}).mount("#app");
