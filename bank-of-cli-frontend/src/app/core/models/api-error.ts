export type ApiErrorCode =
  | 'VALIDATION_ERROR'    // 400
  | 'INVALID_CREDENTIALS' // 401
  | 'UNAUTHORIZED'        // 401
  | 'FORBIDDEN'           // 403
  | 'ACCOUNT_NOT_FOUND'   // 404
  | 'NOT_FOUND'           // 404
  | 'EMAIL_TAKEN'         // 409
  | 'BALANCE_NOT_ZERO'    // 409
  | 'INSUFFICIENT_FUNDS'  // 422
  | 'NETWORK_ERROR'       // 0 (server unreachable)
  | 'UNKNOWN';            // 500 or anything unexpected

/**
 * Every failed request has this body, and every service method
 * fails with this shape. `message` is safe to show in a toast.
 * `field` names the form field at fault, when there is one.
 */
export interface ApiError {
  status: number;
  code: ApiErrorCode;
  message: string;
  field?: string;
}
