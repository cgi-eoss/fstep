import {Component, ViewEncapsulation} from "@angular/core";

import {UserLayer} from '../user/user-layer';
import {ProcessorLayer} from '../../processors/processor-layer';
import {MapService} from '../../map/map.service';
import {UserService} from '../user/user.service';
import {BreadcrumbService} from '../../common/breadcrumb.component/breadcrumb.service';

@Component({
  selector: 'app-root',
  templateUrl: './app.component.html',
  styleUrls: ['./app.component.scss'],
  encapsulation: ViewEncapsulation.None
})
export class AppComponent {
  constructor(mapService: MapService, userService: UserService, breadcrumbService: BreadcrumbService) {
    let userLayer = new UserLayer(mapService, userService);
    breadcrumbService.addFriendlyNameForRouteRegex('^/\\?.*', 'Home');
    //let processorLayer = new ProcessorLayer(mapService, userService);
  }
}