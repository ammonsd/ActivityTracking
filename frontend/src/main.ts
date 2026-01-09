/**
 * Description: Application bootstrap - main entry point for the Angular application
 *
 * Author: Dean Ammons
 * Date: October 2025
 */

import { bootstrapApplication } from '@angular/platform-browser';
import { appConfig } from './app/app.config';
import { AppComponent } from './app/app.component';

bootstrapApplication(AppComponent, appConfig).catch((err) =>
  console.error(err)
);
