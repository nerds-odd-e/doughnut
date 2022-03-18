import HttpResponseError from "./HttpResponseError";
import BadRequestError from "./BadRequestError";
import loginOrRegister from "./loginOrRegister";

type JsonData = Record<string, Record<string, unknown> | unknown>

function objectToFormData(data: JsonData) {
  const formData = new FormData();
  Object.keys(data).forEach((key) => {
    const field = data[key]
    if (field) {
      formData.append(key, '');
    } else if (field instanceof File) {
      formData.append(key, field);
    } else if (field instanceof Object) {
      const fieldRecords = field as Record<string, string | Blob>
      Object.keys(fieldRecords).forEach((subKey) => {
        const subValue = fieldRecords[subKey]
        formData.append(`${key}.${subKey}`, subValue || '');
      });
    } else {
      formData.append(key, field as string);
    }
  });
  return formData;
}
interface RequestOptions{
  method: "GET" | "POST" | "PUT"
  contentType?: 'json'
}
const request = async (url: string, data: JsonData | undefined, { method = "GET", contentType = 'json' }: RequestOptions) => {
  const headers = new Headers();
  headers.set('Accept', 'application/json');
  let body: string | FormData | undefined;
  if (method !== "GET" && data) {
    if (contentType === 'json') {
      headers.set("Content-Type", 'application/json');
      body = JSON.stringify(data)
    }
    else {
      body = objectToFormData(data)
    }
  }
  const res = await fetch(url, { method, headers, body })
  if (res.status === 200 || res.status === 400) {
    return res;
  }
  if (res.status === 204) {
    return { status: 204, json: () => null, text: () => null };
  }
  if (res.status === 401) {
    loginOrRegister();
  }
  throw new HttpResponseError(res.status);
}

class RestfulFetch {
  base_url: string

  constructor(base_url: string) {
    this.base_url = base_url
  }

  private expandUrl(url: string): string {
    if (url.startsWith("/")) return url;
    return this.base_url + url;
  }

  async restRequest(url: string, data: JsonData, params: RequestOptions) {
    const response = await request(this.expandUrl(url), data, params);
    const jsonResponse = await response.json()
    if (response.status === 400) throw new BadRequestError(jsonResponse.errors);
    return jsonResponse;
  }

  async restRequestWithHtmlResponse(url: string, data: JsonData, params: RequestOptions) {
    const response = await request(this.expandUrl(url), data, params)
    if (response.status === 400) throw Error("BadRequest");
    return response.text();
  }
}

export default RestfulFetch;
