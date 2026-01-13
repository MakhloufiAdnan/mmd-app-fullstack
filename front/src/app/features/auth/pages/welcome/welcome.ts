import { Component, inject } from '@angular/core';
import { Router } from '@angular/router';
import { MatButtonModule } from '@angular/material/button';

/**
 * Page d'accueil publique 
 */
@Component({
  selector: 'mdd-welcome',
  standalone: true,
  imports: [MatButtonModule],
  templateUrl: './welcome.html',
  styleUrl: './welcome.scss',
})
export class Welcome {
  private readonly router = inject(Router);

  /* 
  * Navigation via boutons 
  */
  goLogin(): void {
    this.router.navigateByUrl('/login').catch(() => undefined);
  }

  goRegister(): void {
    this.router.navigateByUrl('/register').catch(() => undefined);
  }
}
