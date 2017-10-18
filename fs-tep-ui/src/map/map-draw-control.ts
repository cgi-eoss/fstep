import {Injectable} from '@angular/core';

import DrawInteraction from 'ol/interaction/draw';
import VectorSource from 'ol/source/vector';

import {MapService} from './map.service';

@Injectable()
export class MapDrawControl {

  private ol_control;

  constructor(private map: MapService) {
    map.getViewer().then((viewer)=>{
        this.createOlControl(viewer);
    })
  }

  setEnabled(enabled) {
    this.ol_control.set('active', enabled);
  }

  setMode(mode) {
    this.ol_control.set('type', mode);
  }

  getOlControl() {
    return this.ol_control;
  }

  private createOlControl(viewer) {

    let source = new VectorSource({
    });


    this.ol_control = new DrawInteraction({
      source: source,
      type: 'Point'
    });

    viewer.addInteraction(this.ol_control);
  }
}

