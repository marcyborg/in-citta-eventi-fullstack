import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { AuthService } from './auth.service';
import { provideZonelessChangeDetection } from '@angular/core';

describe('AuthService', () => {
  let auth: AuthService;
  let http: HttpTestingController;
  beforeEach(() => {
    TestBed.configureTestingModule({ providers: [provideZonelessChangeDetection(), provideHttpClient(), provideHttpClientTesting()] });
    auth = TestBed.inject(AuthService);
    http = TestBed.inject(HttpTestingController);
  });
  afterEach(() => http.verify());

  it('registra con username e password', () => {
    auth.register('mario', 'password123').subscribe();
    const req = http.expectOne('/api/auth/register');
    expect(req.request.body).toEqual({ username: 'mario', password: 'password123' });
    req.flush({ message: 'Utente registrato' });
  });

  it('cancella le credenziali alla disconnessione', () => {
    auth.signIn('token', 'mario');
    expect(auth.getToken()).toBe('token');
    auth.logout();
    expect(auth.getToken()).toBeNull();
    expect(auth.profile()).toBeNull();
  });

  it('abilita il proprietario ma non un altro utente o un evento storico', () => {
    auth.signIn('token', 'mario', { id: 3, username: 'mario', role: 'USER' });
    expect(auth.canManageEvent({ ownerId: 3 })).toBeTrue();
    expect(auth.canManageEvent({ ownerId: 4 })).toBeFalse();
    expect(auth.canManageEvent({ ownerId: null })).toBeFalse();
  });

  it('abilita un amministratore anche per eventi senza proprietario', () => {
    auth.signIn('token', 'admin', { id: 5, username: 'admin', role: 'ADMIN' });
    expect(auth.canManageEvent({ ownerId: null })).toBeTrue();
    auth.logout();
    expect(auth.canManageEvent({ ownerId: 5 })).toBeFalse();
  });
});
