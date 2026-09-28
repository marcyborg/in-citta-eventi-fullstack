import { ChangeDetectorRef, Component, OnInit, inject } from '@angular/core';
import { DatePipe } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { EventService } from '../../services/event.service';
import { Event } from '../../models/event.model';

@Component({
  selector: 'app-event-list',
  standalone: true,
  templateUrl: './event-list.html',
  styleUrl: './event-list.scss',
  imports: [DatePipe, FormsModule, RouterLink]
})
export class EventListComponent implements OnInit {
  private readonly service = inject(EventService);
  private readonly cdr = inject(ChangeDetectorRef);
  events: Event[] = [];
  category = '';
  startDate = '';
  endDate = '';
  page = 0;
  readonly size = 9;
  totalPages = 0;
  totalElements = 0;
  loading = false;
  error = '';
  filterError = '';

  ngOnInit(): void { this.loadEvents(); }

  loadEvents(): void {
    if (this.startDate && this.endDate && this.startDate > this.endDate) {
      this.filterError = 'La data iniziale deve precedere quella finale.';
      this.cdr.markForCheck();
      return;
    }
    this.filterError = '';
    this.error = '';
    this.loading = true;
    this.service.search({
      categoria: this.category,
      start: this.startDate ? `${this.startDate}T00:00:00` : undefined,
      end: this.endDate ? `${this.endDate}T23:59:59` : undefined,
      page: this.page,
      size: this.size
    }).subscribe({
      next: result => {
        this.events = result.content;
        this.totalPages = result.totalPages;
        this.totalElements = result.totalElements;
        this.loading = false;
        this.cdr.markForCheck();
      },
      error: () => {
        this.error = 'Impossibile caricare gli eventi. Controlla che il backend sia avviato.';
        this.loading = false;
        this.cdr.markForCheck();
      }
    });
  }

  applyFilters(): void { this.page = 0; this.loadEvents(); }
  reset(): void {
    this.category = '';
    this.startDate = '';
    this.endDate = '';
    this.applyFilters();
  }
  nextPage(): void {
    if (this.page + 1 < this.totalPages) { this.page++; this.loadEvents(); }
  }
  prevPage(): void {
    if (this.page > 0) { this.page--; this.loadEvents(); }
  }
}
