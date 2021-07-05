class HttpResponseError extends Error {
  constructor(status) {
    super(`got ${status}`);
    this.status = status;
  }
}

const reloadSession = () => {
  window.alert("Your login session has expired, let's refresh.");
  window.location.reload();
};

const restRequest = (url, params, loadingRef, callback, errorCallback) => {
  if (loadingRef instanceof Function) {
    loadingRef(true);
  } else {
    loadingRef.value = true;
  }
  fetch(url, params)
    .then((res) => {
      if (res.status !== 200 && res.status != 400) {
        throw new HttpResponseError(res.status);
      }
      return res.json().then((resp) => {
        if (res.status === 200) callback(resp);
        if (res.status === 400) errorCallback(resp.errors);
      });
    })
    .catch((error) => {
      if (error.status === 401) {
        reloadSession()
        return;
      }
      window.alert(error)
    })
    .finally(() => {
      if (loadingRef instanceof Function) {
        loadingRef(false)
      } else {
        loadingRef.value = false;
      }
    });
};

const restGet = (url, loadingRef, callback) => {
  restRequest(url, {}, loadingRef, callback, () => {});
};

const restPost = (url, data, loadingRef, callback) => {
  restRequest(
    url,
    {
      method: 'POST',
      headers: {
        Accept: 'application/json',
        'Content-Type': 'application/json',
      },
      body: JSON.stringify(data),
    },
    loadingRef,
    callback,
    () => {}
  );
};

function objectToFormData(data){
  const formData = new FormData();
  Object.keys(data).forEach(function (key) {
    if (data[key] === null) {
      formData.append(key, '')
    }
    else if(data[key] instanceof Object) {
      Object.keys(data[key]).forEach(function (subKey) {
        formData.append(`${key}.${subKey}`, data[key][subKey] === null ? '' : data[key][subKey]);
      })
    }
    else {
        formData.append(key, data[key])
      }
  });
  return formData
}


const restPostMultiplePartForm = (
  url,
  data,
  loadingRef,
  callback,
  errorCallback
) => {
  restRequest(
    url,
    {
      method: 'POST',
      headers: {
        Accept: 'application/json',
      },
      body: objectToFormData(data),
    },
    loadingRef,
    callback,
    errorCallback
  );
};

export { restGet, restPost, restPostMultiplePartForm };
