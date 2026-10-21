import { User } from './user';

export interface LoginRequest {
  email: string;
  password: string;
}

export interface RegisterRequest {
  name: string;
  email: string;
  password: string; // at least 6 characters
}
/** Body of DELETE /users/me. The user re-enters their password to confirm. */
export interface DeleteUserRequest {
  password: string;
}

/** Returned by both login and register. */
export interface AuthResponse {
  token: string;
  user: User;
}
