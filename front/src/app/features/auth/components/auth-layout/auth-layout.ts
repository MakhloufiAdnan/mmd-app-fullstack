import { Component, Input, inject, PLATFORM_ID } from '@angular/core';
import { isPlatformBrowser, Location } from '@angular/common';
import { Router } from '@angular/router';

import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';

/**
 * Layout des pages d'authentification (Login/Register).
 */
@Component({
  selector: 'mdd-auth-layout',
  standalone: true,
  imports: [MatButtonModule, MatIconModule],
  templateUrl: './auth-layout.html',
  styleUrl: './auth-layout.scss',
})
export class AuthLayout {
  /** Titre de la page  */
  @Input({ required: true }) title!: string;

  private readonly platformId = inject(PLATFORM_ID);
  private readonly location = inject(Location);
  private readonly router = inject(Router);

  /*
   * Navigation retour 
   */
  onBack(): void {
    if (!isPlatformBrowser(this.platformId)) {
      this.router.navigateByUrl('/').catch(() => undefined);
      return;
    }

    const referrer = globalThis.document?.referrer ?? '';
    const origin = globalThis.location?.origin ?? '';
    const sameOriginReferrer = !!referrer && !!origin && referrer.startsWith(origin);

    if (sameOriginReferrer) {
      this.location.back();
      return;
    }

    this.router.navigateByUrl('/').catch(() => undefined);
  }
}
