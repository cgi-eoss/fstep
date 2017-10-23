import {NgModule} from '@angular/core';
import {RouterModule, Routes} from '@angular/router';

import {ProcessorListComponent} from './processor-list.component/processor-list.component';
import {ProcessorViewComponent} from './processor-view.component/processor-view.component';

const processorsRoutes: Routes = [
    {path: 'processor', component: ProcessorListComponent},
    {path: 'processor/:id', component: ProcessorViewComponent}
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