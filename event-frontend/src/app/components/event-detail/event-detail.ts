import { ChangeDetectorRef, Component, OnInit, inject } from '@angular/core';
import { DatePipe } from '@angular/common';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { EventService } from '../../services/event.service';
import { AuthService } from '../../services/auth.service';
import { Event } from '../../models/event.model';
import { EventMapComponent } from '../event-map/event-map';
import { GeocodingService } from '../../services/geocoding.service';

@Component({
  selector: 'app-event-detail',
  standalone: true,
  templateUrl: './event-detail.html',
  styleUrl: './event-detail.scss',
  imports: [DatePipe, RouterLink, EventMapComponent]
})
export class EventDetailComponent implements OnInit {
  readonly encodeURIComponent = encodeURIComponent;
  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);
  private readonly service = inject(EventService);
  private readonly geocoding = inject(GeocodingService);
  private readonly cdr = inject(ChangeDetectorRef);
  readonly auth = inject(AuthService);
  event?: Event;
  loading = true;
  error = '';
  deleting = false;
  locating = false;
  mapError = '';
  mapLatitude?: number | null;
  mapLongitude?: number | null;

  ngOnInit(): void {
    const id = Number(this.route.snapshot.paramMap.get('id'));
    if (!Number.isInteger(id) || id <= 0) { this.error = 'Evento non trovato.'; this.loading = false; return; }
    this.service.getById(id).subscribe({
      next: event => {
        this.event = event;
        this.mapLatitude = event.latitude;
        this.mapLongitude = event.longitude;
        this.loading = false;
        this.cdr.markForCheck();
      },
      error: err => { this.error = err.status === 404 ? 'Evento non trovato.' : 'Impossibile caricare l’evento.'; this.loading = false; this.cdr.markForCheck(); }
    });
  }

  locate(): void {
    if (!this.event?.luogo || this.locating) return;
    this.locating = true;
    this.mapError = '';
    this.geocoding.search(this.event.luogo).subscribe({
      next: result => {
        this.mapLatitude = result.latitude;
        this.mapLongitude = result.longitude;
        this.locating = false;
        this.cdr.markForCheck();
      },
      error: err => {
        this.mapError = err.error?.detail || 'Non siamo riusciti a localizzare questo luogo.';
        this.locating = false;
        this.cdr.markForCheck();
      }
    });
  }

  deleteEvent(): void {
    if (!this.event?.id || !confirm(`Eliminare "${this.event.titolo}"?`)) return;
    this.deleting = true;
    this.service.delete(this.event.id).subscribe({
      next: () => this.router.navigate(['/']),
      error: () => { this.error = 'Impossibile eliminare l’evento.'; this.deleting = false; this.cdr.markForCheck(); }
    });
  }
}
