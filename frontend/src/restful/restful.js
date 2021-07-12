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

function toNested(data) {
  const result = {}
  Object.keys(data).forEach(function (key) {
    if(key.includes(".")) {
      const [namespace, subkey] = key.split('.')
      if(!result[namespace]) result[namespace] = {}
      result[namespace][subkey] = data[key]
    }
    else {
      result[key] = data[key]
    }
  })

  return result
}

const restRequest = (url, params, loadingRef) => {
  if (loadingRef instanceof Function) {
    loadingRef(true);
  } else {
    loadingRef.value = true;
  }

  return new Promise((resolve, reject)=>{
    fetch(url, params)
      .then((res) => {
        if (res.status !== 200 && res.status != 400) {
          throw new HttpResponseError(res.status);
        }
        return res.json().then((resp) => {
          if (res.status === 200) resolve(resp);
          if (res.status === 400) reject(toNested(resp.errors));
        });
      })
      .catch((error) => {
        if (error.status === 401) {
          reloadSession()
          return;
        }
        if (error.status === 404) {
          reject({statusCode: 404})
          return;
        }
        window.alert(error)
      })
  })
  .finally(() => {
    if (loadingRef instanceof Function) {
      loadingRef(false)
    } else {
      loadingRef.value = false;
    }
  });


};

const restGet = (url, loadingRef) => {
  return restRequest(url, {}, loadingRef);
};

const restPost = (url, data, loadingRef) => {
  return restRequest(
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
  )
};

function objectToFormData(data){
  const formData = new FormData();
  Object.keys(data).forEach(function (key) {
    if (data[key] === null) {
      formData.append(key, '')
    }
    else if(data[key] instanceof Object && !(data[key] instanceof File)) {
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


const restPostMultiplePartForm = (url, data, loadingRef) => {
  return restRequest(
    url,
    {
      method: 'POST',
      headers: {
        Accept: 'application/json',
      },
      body: objectToFormData(data),
    },
    loadingRef
  );
};

export { restGet, restPost, restPostMultiplePartForm };
