import  {Injectable} from '@angular/core';

import Map from 'ol/map';
import View from 'ol/view';
import Stamen from 'ol/source/stamen';
import Tile from 'ol/layer/tile';

import 'ol/ol.css';
import "font-awesome/css/font-awesome.css";

@Injectable()
export class MapService {

    private viewer : Promise<Map> = null;
    private resolve: Function = null;

    constructor() {
        this.viewer = new Promise<Map>((resolve, reject) => {
            this.resolve = resolve;
        });
    }

    initMap(container) {

        let map = new Map({
            target: container,
            layers: [
                new Tile({
                    type: 'base',
                    visible: true,
                    source: new Stamen({
                        layer: 'toner'
                    })
                })
            ],
            view: new View({
                center: [0, 0],
                zoom: 6,
                projection: 'EPSG:4326'
            }),
            controls: []
        });

        map.getLayers().item(0).setProperties({
            id: 'base'
        });

        this.resolve(map);
    }

    getViewer() {
        return this.viewer;
    }

    fitExtent(extent) {
        this.getViewer().then((viewer)=>{
            viewer.getView().fit(extent, viewer.getSize());
        });
    }

};
