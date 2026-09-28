import { provideZonelessChangeDetection } from '@angular/core';
import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { GeocodingService } from './geocoding.service';

describe('GeocodingService', () => {
  it('cerca soltanto quando richiesto e codifica il luogo', () => {
    TestBed.configureTestingModule({
      providers: [provideZonelessChangeDetection(), provideHttpClient(), provideHttpClientTesting()]
    });
    const service = TestBed.inject(GeocodingService);
    const http = TestBed.inject(HttpTestingController);
    http.expectNone('/api/geocode');
    service.search(' Piazza del Duomo, Milano ').subscribe(result => {
      expect(result.latitude).toBe(45.46);
    });
    const req = http.expectOne(request => request.url === '/api/geocode');
    expect(req.request.params.get('luogo')).toBe('Piazza del Duomo, Milano');
    req.flush({ latitude: 45.46, longitude: 9.19, displayName: 'Milano' });
    http.verify();
  });
});
