import  {Injectable} from '@angular/core';
import {BehaviorSubject} from 'rxjs/BehaviorSubject';

import events from 'ol/events';
import EventType from 'ol/events/eventtype';
import proj from 'ol/proj';

import {MapService} from './map.service';

@Injectable()
export class CoordService {

    private mouseCoords: BehaviorSubject<{lat: number, lon: number}> = new BehaviorSubject(null);
    constructor(private map: MapService) {
      map.getViewer().then((viewer)=>{
        this.initHandler(viewer);
      })
    }

    private initHandler(viewer) {
      let viewport = viewer.getViewport();
      events.listen(viewport, EventType.MOUSEMOVE, (evt)=>{
        let px = viewer.getEventPixel(evt);
        let coord = viewer.getCoordinateFromPixel(px);
        if (coord) {
          coord = proj.transform(coord, viewer.getView().getProjection(), 'EPSG:4326');

          this.mouseCoords.next({
            lat: coord[1],
            lon: coord[0]
          })
        }
        else {
          this.mouseCoords.next(null);
        }
      });
    }

    public getMouseCoords() {
      return this.mouseCoords.asObservable();
    }

    

};

