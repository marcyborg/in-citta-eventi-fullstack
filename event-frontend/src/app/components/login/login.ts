import { ChangeDetectorRef, Component, inject } from '@angular/core';
import { FormsModule, NgForm } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { AuthService, UserProfile } from '../../services/auth.service';
import { Observable } from 'rxjs';

@Component({
  selector: 'app-login',
  standalone: true,
  templateUrl: './login.html',
  styleUrl: './login.scss',
  imports: [FormsModule, RouterLink]
})
export class LoginComponent {
  private readonly auth = inject(AuthService);
  private readonly router = inject(Router);
  private readonly cdr = inject(ChangeDetectorRef);
  username = '';
  password = '';
  registering = false;
  busy = false;
  error = '';
  notice = '';

  submit(form: NgForm): void {
    if (form.invalid) { form.control.markAllAsTouched(); return; }
    this.busy = true;
    this.error = '';
    const request: Observable<{ token?: string; message?: string; user?: UserProfile }> = this.registering
      ? this.auth.register(this.username.trim(), this.password)
      : this.auth.login(this.username.trim(), this.password);
    request.subscribe({
      next: result => {
        this.busy = false;
        if (typeof result.token === 'string' && result.user) {
          this.auth.signIn(result.token, result.user.username, result.user);
          this.router.navigate(['/']);
        } else if (this.registering) {
          this.notice = 'Registrazione completata. Ora puoi accedere.';
          this.registering = false;
        } else {
          this.error = 'Risposta di accesso non valida. Riprova.';
        }
        this.cdr.markForCheck();
      },
      error: err => {
        this.error = err.error?.detail || 'Operazione non riuscita. Controlla i dati e riprova.';
        this.busy = false;
        this.cdr.markForCheck();
      }
    });
  }
}
