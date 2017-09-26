import {MapService} from '../../map/map.service';
import {UserService} from '../user/user.service';

import VectorSource from 'ol/source/vector';
import VectorLayer from 'ol/layer/vector';
import Feature from 'ol/feature';
import Polygon from 'ol/geom/polygon';
import GeoJSON from 'ol/format/geojson';
import Proj from 'ol/proj';


export class UserLayer {

    private viewer;
    private jsonParser;
    private layer;

    constructor(mapService: MapService, userService: UserService) {
        mapService.getViewer().then((viewer) => {
            this.viewer = viewer;
            this.initLayer();
            this.updateUserArea(userService.getCurrentUser());
        });

        userService.getUser().subscribe((user)=>{
            if (this.viewer) {
                this.updateUserArea(user);
            }
        })
    }

    private initLayer() {
        let source = new VectorSource({});
        this.layer = new VectorLayer({
            source: source
        });

        this.viewer.getLayers().push(this.layer);

        this.jsonParser = new GeoJSON();
    }

    private updateUserArea(user) {
        if (user) {
            let coords = user.subscription.area;
            
            let view = this.viewer.getView();

            let geom = this.jsonParser.readGeometryFromObject({type: 'MultiPolygon', coordinates: coords}, {
                dataProjection: 'EPSG:4326',
                featureProjection: view.getProjection()
            });

            if (geom) {
                let feature = new Feature({
                    geometry: geom
                });

                this.layer.getSource().addFeature(feature);

                let extent = geom.getExtent()

                if (view.getProjection().getCode() != 'EPSG:4326') {
                    extent = Proj.transformExtent(extent,  'EPSG:4326', view.getProjection());
                }   
                view.fit(extent, {
                    maxZoom: Math.max(view.getZoom(), 16),
                    duration: 1000
                });
            }
        }

    }

};
