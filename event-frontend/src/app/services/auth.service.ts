import { Injectable, inject, signal } from '@angular/core';
import { HttpClient, HttpHeaders } from '@angular/common/http';
import { API_BASE } from './api-base';
import { Event } from '../models/event.model';

export interface UserProfile {
  id: number;
  username: string;
  role: 'USER' | 'ADMIN';
}

@Injectable({ providedIn: 'root' })
export class AuthService {
  private readonly http = inject(HttpClient);
  // In-memory credentials deliberately expire on refresh. Never persist JWT in browser storage.
  readonly token = signal<string | null>(null);
  readonly username = signal<string | null>(null);
  readonly profile = signal<UserProfile | null>(null);

  login(username: string, password: string) {
    return this.http.post<{ token: string; user: UserProfile }>(`${API_BASE}/api/auth/login`, { username, password });
  }

  register(username: string, password: string) {
    return this.http.post<{ message: string }>(`${API_BASE}/api/auth/register`, { username, password });
  }

  signIn(token: string, username: string, profile?: UserProfile): void {
    this.token.set(token);
    this.username.set(username);
    this.profile.set(profile ?? null);
  }

  getToken(): string | null {
    return this.token();
  }

  canManageEvent(event: Pick<Event, 'ownerId'>): boolean {
    const user = this.profile();
    return !!this.token() && !!user &&
      (user.role === 'ADMIN' || (event.ownerId != null && event.ownerId === user.id));
  }

  jwtHeader(): HttpHeaders {
    return this.token()
      ? new HttpHeaders({ Authorization: `Bearer ${this.token()}` })
      : new HttpHeaders();
  }

  logout(): void {
    this.token.set(null);
    this.username.set(null);
    this.profile.set(null);
  }
}
