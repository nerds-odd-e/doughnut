const restGet = (url, loadingRef, callback) => {
  loadingRef.value = true
  fetch(url)
    .then(res => {
      return res.json();
    })
    .then(resp => {
      callback(resp)
      loadingRef.value = false
    })
    .catch(error => {
      window.alert(error);
    });
}

export {restGet}