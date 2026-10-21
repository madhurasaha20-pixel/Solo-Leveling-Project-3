import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { BehaviorSubject, Observable, catchError, map, tap, throwError } from 'rxjs';
import { AuthResponse, DeleteUserRequest, LoginRequest, RegisterRequest, User } from '../models';

import { environment } from '../../../environments/environment';
import { toApiError } from './api-error.util';

const SESSION_KEY = 'boc.session';

/**
 * Login, register, logout, and "who is logged in right now".
 *
 * Usage in a component:
 *   this.auth.login({ email, password }).subscribe({
 *     next: user => this.router.navigate(['/dashboard']),
 *     error: (err: ApiError) => this.toast.error(err.message)
 *   });
 *
 *   currentUser$ | async   -> show the user's name in the header
 */
@Injectable({ providedIn: 'root' })
export class AuthService {
  private http = inject(HttpClient);
  private api = environment.apiBase;

  private session: AuthResponse | null = this.loadSession();
  private userSubject = new BehaviorSubject<User | null>(this.session?.user ?? null);

  /** Emits the logged-in user, or null after logout. */
  readonly currentUser$: Observable<User | null> = this.userSubject.asObservable();

  get currentUser(): User | null {
    return this.userSubject.value;
  }

  /** Used by the auth-token interceptor. */
  get token(): string | null {
    return this.session?.token ?? null;
  }

  isLoggedIn(): boolean {
    return this.session !== null;
  }

  /** Errors: VALIDATION_ERROR, INVALID_CREDENTIALS */
  login(req: LoginRequest): Observable<User> {
    return this.http.post<AuthResponse>(`${this.api}/auth/login`, req).pipe(
      tap(res => this.startSession(res)),
      map(res => res.user),
      catchError(err => throwError(() => toApiError(err)))
    );
  }

  /** Creates the user (with no accounts), then logs them in. Errors: VALIDATION_ERROR, EMAIL_TAKEN */
  register(req: RegisterRequest): Observable<User> {
    return this.http.post<AuthResponse>(`${this.api}/auth/register`, req).pipe(
      tap(res => this.startSession(res)),
      map(res => res.user),
      catchError(err => throwError(() => toApiError(err)))
    );
  }

  /** Clears the session immediately. Navigate to /login afterwards. */
  logout(): void {
    this.http.post<void>(`${this.api}/auth/logout`, {}).subscribe({ error: () => { /* already logged out locally */ } });
    this.clearSession();
  }
  deleteAccount(password: string): Observable<void> {
    const body: DeleteUserRequest = { password };
    return this.http.delete<void>(`${this.api}/users/me`, { body }).pipe(
        tap(() => this.clearSession()),
        catchError(err => throwError(() => toApiError(err)))
    );
  }


  private startSession(res: AuthResponse): void {
    this.session = res;
    try { sessionStorage.setItem(SESSION_KEY, JSON.stringify(res)); } catch { /* ignore */ }
    this.userSubject.next(res.user);
  }
  private clearSession(): void {
    this.session = null;
    try { sessionStorage.removeItem(SESSION_KEY); } catch { /* ignore */ }
    this.userSubject.next(null);
  }

  private loadSession(): AuthResponse | null {
    try {
      const raw = sessionStorage.getItem(SESSION_KEY);
      return raw ? (JSON.parse(raw) as AuthResponse) : null;
    } catch {
      return null;
    }
  }
}
