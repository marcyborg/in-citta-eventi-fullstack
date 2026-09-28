import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import { API_BASE } from './api-base';

export interface GeocodedLocation {
  latitude: number;
  longitude: number;
  displayName: string;
}

@Injectable({ providedIn: 'root' })
export class GeocodingService {
  private readonly http = inject(HttpClient);

  search(place: string): Observable<GeocodedLocation> {
    return this.http.get<GeocodedLocation>(`${API_BASE}/api/geocode`, {
      params: new HttpParams().set('luogo', place.trim())
    });
  }
}
