class HttpResponseError extends Error {
  status;

  message;

  constructor(status, message) {
    super(`got ${status}`);
    this.status = status;
    this.message = message;
  }
}

export default HttpResponseError;
