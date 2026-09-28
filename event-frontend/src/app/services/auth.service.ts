import { Injectable, inject, signal } from '@angular/core';
import { HttpClient, HttpHeaders } from '@angular/common/http';
import { API_BASE } from './api-base';

@Injectable({ providedIn: 'root' })
export class AuthService {
  private readonly http = inject(HttpClient);
  // In-memory credentials deliberately expire on refresh. Never persist JWT in browser storage.
  readonly token = signal<string | null>(null);
  readonly username = signal<string | null>(null);

  login(username: string, password: string) {
    return this.http.post<{ token: string }>(`${API_BASE}/api/auth/login`, { username, password });
  }

  register(username: string, password: string) {
    return this.http.post<{ message: string }>(`${API_BASE}/api/auth/register`, { username, password });
  }

  signIn(token: string, username: string): void {
    this.token.set(token);
    this.username.set(username);
  }

  getToken(): string | null {
    return this.token();
  }

  jwtHeader(): HttpHeaders {
    return this.token()
      ? new HttpHeaders({ Authorization: `Bearer ${this.token()}` })
      : new HttpHeaders();
  }

  logout(): void {
    this.token.set(null);
    this.username.set(null);
  }
}
