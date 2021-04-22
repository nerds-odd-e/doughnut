
var app = new Vue({
  el: '#app',
  data: {
    blogInfo: [],
    articleList: [],
    apiUrl: "../api/blog_articles_by_website_name/odd-e-blog"
  },
  created: function() {
    this.blogInfo.push(
    {
        "year": "2017",
        "link": "abc"
    });
    this.blogInfo.push(
    {
        "year": "2018",
        "link": "abc"
    });
    this.blogInfo.push(
    {
        "year": "2019",
        "link": "abc"
    });
    this.blogInfo.push(
    {
        "year": "2020",
        "link": "abc"
    });
    this.blogInfo.push(
    {
        "year": "2021",
        "link": "abc"
    });
    axios
        .get(this.apiUrl)
            .then((res) => {
                this.articleList = res.data;
            })
            .catch((res) => {
                alert("Error");
            });
  }
});