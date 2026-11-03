import { AfterViewInit, Component, ElementRef, OnInit, inject, signal, viewChild } from '@angular/core';
import { HttpErrorResponse, HttpStatusCode } from '@angular/common/http';
import { FormControl, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';

import { TranslatePipe } from '@ngx-translate/core';

import { AccountService } from 'app/core/auth';
import { LoginService } from 'app/login/login.service';
import { TranslateDirective } from 'app/shared/language';

/** Il backend risponde 429 quando scatta il rate limit sui tentativi di accesso. */
const TOO_MANY_REQUESTS: number = HttpStatusCode.TooManyRequests;

@Component({
  selector: 'jhi-login',
  imports: [TranslateDirective, TranslatePipe, ReactiveFormsModule, RouterLink],
  templateUrl: './login.html',
})
export default class Login implements OnInit, AfterViewInit {
  readonly username = viewChild.required<ElementRef>('username');

  readonly authenticationError = signal(false);
  /** Vero quando il backend ha respinto il tentativo per rate limit (429). */
  readonly rateLimited = signal(false);

  loginForm = new FormGroup({
    username: new FormControl('', { nonNullable: true, validators: [Validators.required] }),
    password: new FormControl('', { nonNullable: true, validators: [Validators.required] }),
    rememberMe: new FormControl(false, { nonNullable: true, validators: [Validators.required] }),
  });

  private readonly accountService = inject(AccountService);
  private readonly loginService = inject(LoginService);
  private readonly router = inject(Router);

  ngOnInit(): void {
    // if already authenticated then navigate to home page
    this.accountService.identity().subscribe(() => {
      if (this.accountService.isAuthenticated()) {
        this.router.navigate(['']);
      }
    });
  }

  ngAfterViewInit(): void {
    this.username().nativeElement.focus();
  }

  login(): void {
    this.loginService.login(this.loginForm.getRawValue()).subscribe({
      next: () => {
        this.authenticationError.set(false);
        this.rateLimited.set(false);
        if (!this.router.currentNavigation()) {
          // There were no routing during login (eg from navigationToStoredUrl)
          this.router.navigate(['']);
        }
      },
      error: (response: HttpErrorResponse) => {
        const tooManyRequests = response.status === TOO_MANY_REQUESTS;
        this.rateLimited.set(tooManyRequests);
        this.authenticationError.set(!tooManyRequests);
      },
    });
  }
}
