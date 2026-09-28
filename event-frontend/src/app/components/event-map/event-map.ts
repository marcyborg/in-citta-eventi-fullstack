import { AfterViewInit, Component, ElementRef, EventEmitter, Input, OnChanges, OnDestroy, Output, SimpleChanges, ViewChild } from '@angular/core';
import * as L from 'leaflet';

@Component({
  selector: 'app-event-map',
  standalone: true,
  template: '<div #mapHost class="map-host" [attr.aria-label]="selectable ? \'Mappa interattiva: clicca per spostare il pin\' : \'Mappa del luogo dell’evento\'"></div>',
  styles: [`
    :host { display: block; width: 100%; }
    .map-host { width: 100%; height: 340px; border-radius: 8px; background: var(--color-tint); }
    @media (max-width: 600px) { .map-host { height: 280px; } }
  `]
})
export class EventMapComponent implements AfterViewInit, OnChanges, OnDestroy {
  @Input() latitude: number | null | undefined;
  @Input() longitude: number | null | undefined;
  @Input() selectable = false;
  @Output() positionChange = new EventEmitter<{ latitude: number; longitude: number }>();
  @ViewChild('mapHost', { static: true }) mapHost!: ElementRef<HTMLElement>;

  private map?: L.Map;
  private pin?: L.CircleMarker;

  ngAfterViewInit(): void {
    const point: L.LatLngExpression = this.hasCoordinates()
      ? [this.latitude!, this.longitude!]
      : [42.5, 12.5];
    this.map = L.map(this.mapHost.nativeElement, { scrollWheelZoom: false })
      .setView(point, this.hasCoordinates() ? 15 : 5);
    L.tileLayer('https://tile.openstreetmap.org/{z}/{x}/{y}.png', {
      attribution: '&copy; <a href="https://www.openstreetmap.org/copyright" target="_blank" rel="noopener">OpenStreetMap</a> contributors',
      maxZoom: 19
    }).addTo(this.map);
    if (this.hasCoordinates()) this.setPin(point);
    if (this.selectable) {
      this.map.on('click', event => {
        this.setPin(event.latlng);
        this.map?.setView(event.latlng, Math.max(this.map.getZoom(), 13));
        this.positionChange.emit({ latitude: event.latlng.lat, longitude: event.latlng.lng });
      });
    }
    // The map is introduced conditionally after the geocoding request.
    requestAnimationFrame(() => this.map?.invalidateSize());
  }

  ngOnChanges(changes: SimpleChanges): void {
    if (!this.map) return;
    if (changes['latitude'] || changes['longitude']) {
      if (!this.hasCoordinates()) {
        this.pin?.remove();
        this.pin = undefined;
        return;
      }
      const point: L.LatLngExpression = [this.latitude!, this.longitude!];
      this.map.setView(point, this.map.getZoom());
      this.setPin(point);
    }
  }

  ngOnDestroy(): void {
    this.map?.remove();
  }

  private hasCoordinates(): boolean {
    return this.latitude != null && this.longitude != null
      && Number.isFinite(this.latitude) && Number.isFinite(this.longitude);
  }

  private setPin(point: L.LatLngExpression): void {
    if (this.pin) this.pin.setLatLng(point);
    else if (this.map) this.pin = L.circleMarker(point, {
      radius: 9, color: '#fffefa', weight: 3, fillColor: '#145c50', fillOpacity: 1
    }).addTo(this.map);
  }
}
