import {NgModule} from '@angular/core';
import {RouterModule, Routes} from '@angular/router';

import {ServiceListComponent} from './service-list.component/service-list.component';
import {ProcessorListComponent} from './processor-list.component/processor-list.component';
import {ProcessorViewComponent} from './processor-view.component/processor-view.component';

const processorsRoutes: Routes = [
    {path: 'products', component: ServiceListComponent},
    {path: 'products/:service', component: ProcessorListComponent},
    {path: 'products/:service/:id', component: ProcessorViewComponent}
];

@NgModule({
    imports: [
        RouterModule.forChild(
            processorsRoutes
        )
    ],
    exports: [
        RouterModule
    ]
})
export class ProcessorsRoutingModule {}