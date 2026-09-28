import { Routes } from '@angular/router';
import { EventListComponent } from './components/event-list/event-list';
import { EventDetailComponent } from './components/event-detail/event-detail';
import { EventFormComponent } from './components/event-form/event-form';
import { LoginComponent } from './components/login/login';

export const routes: Routes = [
  { path: '', component: EventListComponent },
  { path: 'events', redirectTo: '', pathMatch: 'full' },
  { path: 'events/:id', component: EventDetailComponent },
  { path: 'new-event', component: EventFormComponent },
  { path: 'login', component: LoginComponent },
  { path: '**', redirectTo: '' }
];
