import  {Injectable} from '@angular/core';

import Map from 'ol/map';
import View from 'ol/view';
import Stamen from 'ol/source/stamen';
import XYZSource from 'ol/source/xyz';

import Tile from 'ol/layer/tile';
import Proj from 'ol/proj';

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

        let mapBoxUrl = 'https://api.mapbox.com/styles/v1/mapbox/streets-v8/tiles/{z}/{x}/{y}?access_token=pk.eyJ1IjoidmFuemV0dGVucCIsImEiOiJjaXZiZTM3Y2owMDdqMnVwa2E1N2VsNGJnIn0.A9BNRSTYajN0fFaVdJIpzQ';

        let map = new Map({
            target: container,
            layers: [
                new Tile({
                    type: 'base',
                    visible: true,
                    // source: new Stamen({
                    //     layer: 'toner'
                    // })
                    source: new XYZSource({
                        tileSize: [512, 512],
                        url: mapBoxUrl
                    })
                })
            ],
            view: new View({
                center: [0, 0],
                zoom: 6,
                projection: 'EPSG:900913'
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
            let mapProj = viewer.getView().getProjection().getCode();
            let fitExtent = extent;
            if (mapProj !== 'EPSG:4326') {
                fitExtent = Proj.transformExtent(extent, 'EPSG:4326', mapProj);
            }
            viewer.getView().fit(fitExtent, {
                duration: 500
            });
        });
    }

};
