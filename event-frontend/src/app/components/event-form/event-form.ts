import { ChangeDetectorRef, Component, OnInit, inject } from '@angular/core';
import { FormsModule, NgForm } from '@angular/forms';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { Event } from '../../models/event.model';
import { EventService } from '../../services/event.service';
import { AuthService } from '../../services/auth.service';
import { GeocodingService } from '../../services/geocoding.service';
import { EventMapComponent } from '../event-map/event-map';

@Component({
  selector: 'app-event-form',
  standalone: true,
  templateUrl: './event-form.html',
  styleUrl: './event-form.scss',
  imports: [FormsModule, RouterLink, EventMapComponent]
})
export class EventFormComponent implements OnInit {
  readonly auth = inject(AuthService);
  private readonly service = inject(EventService);
  private readonly router = inject(Router);
  private readonly route = inject(ActivatedRoute);
  private readonly cdr = inject(ChangeDetectorRef);
  private readonly geocoding = inject(GeocodingService);
  event: Event = { titolo: '', descrizione: '', data: '', luogo: '', categoria: '' };
  error = '';
  saving = false;
  locating = false;
  manualMode = false;
  mapError = '';
  locatedPlace = '';
  manualLatitude = '';
  manualLongitude = '';
  editingId?: number;
  minDate = new Date(Date.now() - new Date().getTimezoneOffset() * 60000).toISOString().slice(0, 16);

  ngOnInit(): void {
    const id = Number(this.route.snapshot.queryParamMap.get('edit'));
    if (Number.isInteger(id) && id > 0) {
      this.editingId = id;
      this.service.getById(id).subscribe({
        next: event => {
          this.event = { ...event, data: event.data.slice(0, 16) };
          this.manualLatitude = event.latitude?.toFixed(6) ?? '';
          this.manualLongitude = event.longitude?.toFixed(6) ?? '';
          this.cdr.markForCheck();
        },
        error: () => { this.error = 'Impossibile caricare l’evento da modificare.'; this.cdr.markForCheck(); }
      });
    }
  }

  clearPosition(): void {
    this.event.latitude = null;
    this.event.longitude = null;
    this.locatedPlace = '';
    this.manualLatitude = '';
    this.manualLongitude = '';
    this.mapError = '';
  }

  locate(): void {
    if (!this.event.luogo.trim() || this.locating) {
      this.mapError = 'Inserisci prima il luogo dell’evento.';
      return;
    }
    this.locating = true;
    this.mapError = '';
    this.geocoding.search(this.event.luogo).subscribe({
      next: result => {
        this.event.latitude = result.latitude;
        this.event.longitude = result.longitude;
        this.manualLatitude = result.latitude.toFixed(6);
        this.manualLongitude = result.longitude.toFixed(6);
        this.locatedPlace = result.displayName;
        this.locating = false;
        this.cdr.markForCheck();
      },
      error: err => {
        this.mapError = err.error?.detail || 'Impossibile cercare il luogo sulla mappa.';
        this.locating = false;
        this.cdr.markForCheck();
      }
    });
  }

  movePin(point: { latitude: number; longitude: number }): void {
    this.event.latitude = point.latitude;
    this.event.longitude = point.longitude;
    this.manualLatitude = point.latitude.toFixed(6);
    this.manualLongitude = point.longitude.toFixed(6);
    this.locatedPlace = 'Pin aggiornato manualmente';
    this.cdr.markForCheck();
  }

  showManualMap(): void {
    this.manualMode = true;
  }

  applyCoordinates(): void {
    const latitude = Number(this.manualLatitude);
    const longitude = Number(this.manualLongitude);
    if (!this.manualLatitude.trim() || !this.manualLongitude.trim() ||
        !Number.isFinite(latitude) || !Number.isFinite(longitude) ||
        latitude < -90 || latitude > 90 || longitude < -180 || longitude > 180) {
      this.mapError = 'Inserisci latitudine (-90/+90) e longitudine (-180/+180) valide.';
      return;
    }
    this.mapError = '';
    this.locatedPlace = 'Coordinate inserite manualmente';
    this.event.latitude = latitude;
    this.event.longitude = longitude;
    this.manualMode = true;
    this.cdr.markForCheck();
  }

  save(form: NgForm): void {
    if (form.invalid || !this.event.titolo.trim() || !this.event.luogo.trim() ||
        !this.event.data || new Date(this.event.data).getTime() <= Date.now()) {
      form.control.markAllAsTouched();
      this.error = 'Compila i campi obbligatori e scegli una data futura.';
      return;
    }
    this.error = '';
    this.saving = true;
    const payload = { ...this.event, titolo: this.event.titolo.trim(),
      luogo: this.event.luogo.trim(), data: `${this.event.data.slice(0, 16)}:00` };
    const request = this.editingId
      ? this.service.update(this.editingId, payload)
      : this.service.create(payload);
    request.subscribe({
      next: event => this.router.navigate(['/events', event.id]),
      error: err => {
        this.error = err.error?.detail || 'Impossibile salvare l’evento. Riprova.';
        this.saving = false;
        this.cdr.markForCheck();
      }
    });
  }
}
