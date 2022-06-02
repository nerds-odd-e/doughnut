class HttpResponseError extends Error {
  status;

  constructor(status) {
    super(`got ${status}`);
    this.status = status;
  }
}

export default HttpResponseError;
