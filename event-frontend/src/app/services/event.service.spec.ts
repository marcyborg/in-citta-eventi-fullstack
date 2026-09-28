import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { EventService } from './event.service';
import { AuthService } from './auth.service';
import { provideZonelessChangeDetection } from '@angular/core';

describe('EventService', () => {
  let service: EventService;
  let http: HttpTestingController;
  let auth: AuthService;

  beforeEach(() => {
    TestBed.configureTestingModule({ providers: [provideZonelessChangeDetection(), provideHttpClient(), provideHttpClientTesting()] });
    service = TestBed.inject(EventService);
    http = TestBed.inject(HttpTestingController);
    auth = TestBed.inject(AuthService);
  });

  afterEach(() => http.verify());

  it('combina categoria, intervallo e ordinamento cronologico', () => {
    service.search({ categoria: 'teatro', start: '2030-10-01T00:00:00', end: '2030-10-31T23:59:59', page: 1 })
      .subscribe();
    const req = http.expectOne(r => r.url === '/api/events');
    expect(req.request.params.get('categoria')).toBe('teatro');
    expect(req.request.params.get('start')).toBe('2030-10-01T00:00:00');
    expect(req.request.params.get('end')).toBe('2030-10-31T23:59:59');
    expect(req.request.params.get('sort')).toBe('data,asc');
    expect(req.request.params.get('page')).toBe('1');
    req.flush({ content: [], totalElements: 0, totalPages: 0 });
  });

  it('aggiunge il bearer token alla creazione', () => {
    auth.signIn('test-token', 'tester');
    service.create({ titolo: 'Mostra', descrizione: '', data: '2030-01-01T18:00:00',
      luogo: 'Milano', categoria: 'altro' }).subscribe();
    const req = http.expectOne('/api/events');
    expect(req.request.headers.get('Authorization')).toBe('Bearer test-token');
    req.flush({ id: 1 });
  });
});
