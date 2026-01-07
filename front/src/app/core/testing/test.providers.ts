import type { EnvironmentProviders, Provider } from '@angular/core';
import { provideHttpClient } from '@angular/common/http';
import { provideHttpClientTesting } from '@angular/common/http/testing';
import { provideRouter } from '@angular/router';

export type TestProviders = Array<Provider | EnvironmentProviders>;

/** Router minimal pour <router-outlet> / Router / ActivatedRoute en tests */
export const ROUTER_TEST_PROVIDERS: TestProviders = [provideRouter([])];

/** HttpClient mockable via HttpTestingController */
export const HTTP_TEST_PROVIDERS: TestProviders = [provideHttpClient(), provideHttpClientTesting()];

/** Par défaut pour les specs de composants */
export const DEFAULT_COMPONENT_TEST_PROVIDERS: TestProviders = [
  ...ROUTER_TEST_PROVIDERS,
  ...HTTP_TEST_PROVIDERS,
];
