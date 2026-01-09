/**
 * Description: Application configuration - provides Angular application configuration including routing, HTTP client, and interceptors
 *
 * Author: Dean Ammons
 * Date: January 2026
 */

import { ApplicationConfig, provideZoneChangeDetection } from '@angular/core';
import { provideRouter } from '@angular/router';
import {
  provideHttpClient,
  withInterceptors,
  withInterceptorsFromDi,
} from '@angular/common/http';
import { provideAnimations } from '@angular/platform-browser/animations';
import { provideNativeDateAdapter } from '@angular/material/core';
import { MatIconRegistry } from '@angular/material/icon';
import { DomSanitizer } from '@angular/platform-browser';

import { routes } from './app.routes';
import { authInterceptor } from './interceptors/auth.interceptor';

// Configure Material Icons to use SVG instead of font ligatures
function configureIcons(
  iconRegistry: MatIconRegistry,
  sanitizer: DomSanitizer
) {
  iconRegistry.setDefaultFontSetClass('material-symbols-outlined');
  return () => {};
}

export const appConfig: ApplicationConfig = {
  providers: [
    provideZoneChangeDetection({ eventCoalescing: true }),
    provideRouter(routes),
    provideHttpClient(
      withInterceptors([authInterceptor]),
      withInterceptorsFromDi()
    ),
    provideAnimations(),
    provideNativeDateAdapter(),
    {
      provide: 'ICON_INIT',
      useFactory: configureIcons,
      deps: [MatIconRegistry, DomSanitizer],
      multi: true,
    },
  ],
};
