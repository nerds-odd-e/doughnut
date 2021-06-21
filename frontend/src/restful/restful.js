const restGet = (url, loadingRef, callback) => {
  if (loadingRef instanceof Function) {loadingRef(true)}
  else { loadingRef.value = true }
  fetch(url)
    .then(res => {
      return res.json();
    })
    .then(resp => {
      callback(resp)
      if (loadingRef instanceof Function) {loadingRef(false)}
      else { loadingRef.value = false }
    })
    .catch(error => {
      window.alert(error);
    });
}

export {restGet}