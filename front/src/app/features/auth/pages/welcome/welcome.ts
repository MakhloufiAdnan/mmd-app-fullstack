import { Component, inject } from '@angular/core';
import { Router } from '@angular/router';

/**
 * Page d'accueil publique 
 */
@Component({
  selector: 'mdd-welcome',
  standalone: true,
  templateUrl: './welcome.html',
  styleUrl: './welcome.scss',
})
export class Welcome {
  private readonly router = inject(Router);

  /* 
  * Navigation via boutons 
  */
  goLogin(): void {
    void this.router.navigateByUrl('/login');
  }

  goRegister(): void {
    void this.router.navigateByUrl('/register');
  }
}
