
var app = new Vue({
  el: '#app',
  data: {
    blogInfo: [],
    articleList: [],
    apiUrl: "../api/note/blog"
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
    this.articleList.push({
        "title" : "Hello World",
        "authorName" : "John Doe",
        "createdAt" : "20th December 2021",
        "content" : "This is first article's content"
    });
    /*axios
        .get(this.apiUrl)
            .then((res) => {
                this.blogInfo = res.data;
            })
            .catch((res) => {
                alert("Error");
            });*/
  }
});