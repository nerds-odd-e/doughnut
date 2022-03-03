import HttpResponseError from "./HttpResponseError";
import BadRequestError from "./BadRequestError";

const loginOrRegister = () => {
  window.location = `/users/identify?from=${window.location.href}`;
};

const request = async (url, {method, contentType='json', body}) => {
  const headers = {Accept: 'application/json' };
  if (contentType === 'json') {
    headers["Content-Type"] = 'application/json';
  }
  const res = await fetch(url, {method, headers, body})
  if (res.status === 200 || res.status === 400) {
    return res;
  }
  if (res.status === 204) {
    return {json: ()=>null, text: ()=>null};
  }
  if (res.status === 401) {
    loginOrRegister();
  }
  throw new HttpResponseError(res.status);
}

const restRequest = async (url, params) => {
  const response = await request(url, params);
  const jsonResponse = await response.json()
  if (response.status === 400) throw new BadRequestError(jsonResponse.errors);
  return jsonResponse;
}

const restRequestWithHtmlResponse = async (url, params) => {
  const response = await request(url, params)
  const html = await response.text();
  if (response.status === 400) throw Error("BadRequest", html);
  return html;
}

const restGet = (url) => restRequest(url, {});

const restPost = (url, data) =>
  restRequest(
    url,
    {
      method: 'POST',
      body: JSON.stringify(data),
    },
  );

const restPatch = (url, data) =>
  restRequest(
    url,
    {
      method: 'PATCH',
      body: JSON.stringify(data),
    },
  );

function objectToFormData(data) {
  const formData = new FormData();
  Object.keys(data).forEach((key) => {
    if (data[key] === null) {
      formData.append(key, '');
    } else if (data[key] instanceof Object && !(data[key] instanceof File)) {
      Object.keys(data[key]).forEach((subKey) => {
        formData.append(
          `${key}.${subKey}`,
          data[key][subKey] === null ? '' : data[key][subKey]
        );
      });
    } else {
      formData.append(key, data[key]);
    }
  });
  return formData;
}

const restPostMultiplePartForm = (url, data) =>
  restRequest(
    url,
    {
      method: 'POST',
      contentType: "MultiplePartForm",
      body: objectToFormData(data),
    },
  );

const restPatchMultiplePartForm = (url, data) =>
  restRequest(
    url,
    {
      method: 'PATCH',
      contentType: "MultiplePartForm",
      body: objectToFormData(data),
    },
  );

const restPostWithHtmlResponse = (url, data) =>
  restRequestWithHtmlResponse(url, {
    method: 'POST',
    body: JSON.stringify(data),
  });

export {
  restGet,
  restPost,
  restPatch,
  restPostMultiplePartForm,
  restPatchMultiplePartForm,
  loginOrRegister,
  restPostWithHtmlResponse,
};
