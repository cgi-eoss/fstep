import {NgModule} from '@angular/core';
import {CommonModule} from '@angular/common';

import {LoaderOverlay} from './loader-overlay.component/loader-overlay.component';
import {BreadcrumbService} from './breadcrumb.component/breadcrumb.service';
import {BreadcrumbComponent} from './breadcrumb.component/breadcrumb.component';

@NgModule({
  declarations: [LoaderOverlay, BreadcrumbComponent],
  imports: [CommonModule],
  exports: [LoaderOverlay, BreadcrumbComponent],
  providers: [BreadcrumbService]
})
export class CommonComponentsModule {}