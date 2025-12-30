import { Component, inject } from '@angular/core';
import { Router, RouterLink, RouterLinkActive, RouterOutlet } from '@angular/router';
import { firstValueFrom } from 'rxjs';

import { MatToolbarModule } from '@angular/material/toolbar';
import { MatSidenav, MatSidenavModule } from '@angular/material/sidenav';

import { AuthFacade } from '@features/auth/state/auth.facade';

/**
 * Layout protégé.
 */
@Component({
  selector: 'app-auth-shell',
  standalone: true,
  imports: [RouterOutlet, RouterLink, RouterLinkActive, MatToolbarModule, MatSidenavModule],
  templateUrl: './auth-shell.html',
  styleUrl: './auth-shell.scss',
})
export class AuthShell {
  private readonly auth = inject(AuthFacade);
  private readonly router = inject(Router);

  /**
   * Déconnecte l'utilisateur :
   * - appelle POST /api/auth/logout (cookie + CSRF) via AuthFacade
   * - purge du token en mémoire (gérée dans AuthFacade)
   * - redirection vers l'accueil public
   *
   * @param drawer tiroir à fermer pour une UX propre
   */
  async logout(drawer: MatSidenav): Promise<void> {
    const logout$ = this.auth.logout(); 
    await firstValueFrom(logout$);

    if (drawer.opened) {
      await drawer.close();
    }

    await this.router.navigateByUrl('/');
  }

  /**
   * Ferme le tiroir après navigation (mobile).
   *
   * @param drawer instance MatSidenav
   */
  async closeDrawer(drawer: MatSidenav): Promise<void> {
    if (drawer.opened) {
      await drawer.close();
    }
  }
}
