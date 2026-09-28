import { Injectable, inject } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { Event } from '../models/event.model';
import { Page } from '../models/page.model';
import { AuthService } from './auth.service';
import { API_BASE } from './api-base';

export interface EventFilters {
  categoria?: string;
  start?: string;
  end?: string;
  page?: number;
  size?: number;
}

@Injectable({ providedIn: 'root' })
export class EventService {
  private readonly http = inject(HttpClient);
  private readonly auth = inject(AuthService);
  private readonly apiUrl = `${API_BASE}/api/events`;

  search(filters: EventFilters = {}): Observable<Page<Event>> {
    let params = new HttpParams()
      .set('page', filters.page ?? 0)
      .set('size', filters.size ?? 10)
      .set('sort', 'data,asc');
    if (filters.categoria) params = params.set('categoria', filters.categoria);
    if (filters.start) params = params.set('start', filters.start);
    if (filters.end) params = params.set('end', filters.end);
    return this.http.get<Page<Event>>(this.apiUrl, { params });
  }

  getById(id: number): Observable<Event> {
    return this.http.get<Event>(`${this.apiUrl}/${id}`);
  }

  create(event: Event): Observable<Event> {
    return this.http.post<Event>(this.apiUrl, event, { headers: this.auth.jwtHeader() });
  }

  update(id: number, event: Event): Observable<Event> {
    return this.http.put<Event>(`${this.apiUrl}/${id}`, event, { headers: this.auth.jwtHeader() });
  }

  delete(id: number): Observable<void> {
    return this.http.delete<void>(`${this.apiUrl}/${id}`, { headers: this.auth.jwtHeader() });
  }
}
