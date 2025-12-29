import { Component } from '@angular/core';

/**
 * Raison d'être :
 * - Permet de vérifier rapidement le parcours UI après login
 * - Sert de point d'atterrissage "connecté". Todo: d'implémenter
 *   la vraie feature feed et la sécurité.
 */
@Component({
  selector: 'mdd-feed',
  standalone: true,
  template: `
    <section style="padding:24px 16px">
      <h1>Feed</h1>
      <p>Placeholder (Phase 1). La protection + session persistante arrive en Phase 2.</p>
    </section>
  `,
})
export class Feed {}
