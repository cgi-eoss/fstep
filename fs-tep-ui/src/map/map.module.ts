import {NgModule} from '@angular/core';
import {CommonModule} from '@angular/common';

import {MapService} from './map.service';
import {AoiLayer} from './aoi-layer';
import {MapComponent} from './map.component';
import {CoordService} from './coord.service';
import {MapDrawControl} from './map-draw-control';
import {LongitudePipe} from './longitude.pipe';
import {LatitudePipe} from './latitude.pipe';

import {MouseCoordsComponent} from './mouse-coords.component/mouse-coords.component';
import { WmtsDomainDiscoveryService } from './services';

@NgModule({
  declarations: [MapComponent, MouseCoordsComponent, LongitudePipe, LatitudePipe],
  imports: [CommonModule],
  exports: [MapComponent, MouseCoordsComponent, LongitudePipe, LatitudePipe],
  providers: [MapService, AoiLayer, CoordService, MapDrawControl, WmtsDomainDiscoveryService]
})
export class MapModule {}