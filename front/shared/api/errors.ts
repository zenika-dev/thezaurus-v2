export class ApiError extends Error {
  readonly status: number;

  constructor(status: number, message: string) {
    super(`${message} (HTTP ${status})`);
    this.name = "ApiError";
    this.status = status;
  }
}
