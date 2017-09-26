import {Injectable} from '@angular/core';
import {Http, Response, URLSearchParams} from '@angular/http';

import {BehaviorSubject} from 'rxjs/BehaviorSubject';

import {MapService} from '../map/map.service';
import {UserService} from '../app/user/user.service';
import {TimeService} from '../app/time.service';

import TileLayer from 'ol/layer/tile';
import ImageLayer from 'ol/layer/image';
import MultiPolygon from 'ol/geom/multipolygon';
import Proj from 'ol/proj';
import OSM from 'ol/source/osm';
import FillStyle from 'ol/style/fill';


@Injectable()
export class ProcessorLayer {

    private viewer;
    private clipGeom;
    private layer;
    
    private state = new BehaviorSubject({visible: false, loading: false, dataSource: null});

    constructor(mapService: MapService, userService: UserService, private timeService: TimeService, private http: Http) {
        mapService.getViewer().then((viewer) => {
            this.viewer = viewer;
            this.initLayer();
            this.updateUserArea(userService.getCurrentUser());
        });

        userService.getUser().subscribe((user)=>{
            this.updateUserArea(user);
        });

        timeService.getSelectedDate().subscribe((dt)=>{
            let ds = this.state.value.dataSource;
            if (ds) {
                ds.updateSourceTime(dt);
            }
        })
    }

    private initLayer() {

        let dataSource = this.state.value.dataSource;
        if (dataSource && dataSource.isTiled()) {
            this.layer = new TileLayer({
                source: dataSource.getOLSource()
            });
        }
        else {
            this.layer = new ImageLayer({
                source: dataSource ? dataSource.getOLSource() : null
            });
        }



        let fillStyle = new FillStyle({color: [0, 0, 0, 0]});
        this.layer.on('precompose', (event) => {
            var ctx = event.context;
            var vecCtx = event.vectorContext;
            ctx.save();
            // Using a style is a hack to workaround a limitation in
            // OpenLayers 3, where a geometry will not be draw if no
            // style has been provided.
            vecCtx.setFillStrokeStyle(fillStyle, null);
            vecCtx.drawGeometry(this.clipGeom);
            ctx.clip();
        });
        
        this.layer.on('postcompose', (event) => {
          var ctx = event.context;
          ctx.restore();
        });

        this.viewer.getLayers().push(this.layer);

        this.setVisible(this.state.value.visible);

    }

    private updateUserArea(user) {
        if (user) {
            let coords = user.subscription.area;
            this.clipGeom = new MultiPolygon(coords);

            if (this.layer) {
                this.layer.setExtent(this.clipGeom.getExtent());
            }
        }

    }

    setDataSource(source) {
        
        this.state.next(Object.assign({}, this.state.value, {
            dataSource: source
        }));


        if (this.layer) {
            if (source && source.isTiled() && this.layer instanceof ImageLayer)  {
                this.viewer.getLayers().remove(this.layer);
                this.initLayer();
            }
            else if (source && !source.isTiled() && this.layer instanceof TileLayer) {
                this.viewer.getLayers().remove(this.layer);
                this.initLayer();
            }
            else {
                this.layer.setSource(source ? source.getOLSource() : null);
            }
        }

        if (source)
            source.updateSourceTime(this.timeService.getCurrentDate());


    }

    setVisible(visible: boolean) {
        this.state.next(Object.assign({}, this.state.value, {
            visible: visible
        }));
        if (this.layer) {
            this.layer.setVisible(visible);
        }
    }

    getCurrentState() {
        return this.state.value;
    }

    getState() {
        return this.state.asObservable();
    }

    getSourceValue(coordinate) {
        let url = this.state.value.dataSource.getFeatureInfo(coordinate, this.viewer.getView().getResolution());

        return this.http.get(url)
            .toPromise()
            .then((response)=>{
                let features =  response.json().features;
                if (features.length) {
                    return features[0].properties.GRAY_INDEX;
                }
                else {
                    return 'No data';
                }
            })
            .catch((error)=>{
                return Promise.reject(error.message || error);
            });
    }

    getSourceTimeSeries(coordinate, start, end) {
        let url = this.state.value.dataSource.getFeatureInfo(coordinate, this.viewer.getView().getResolution());

        url = url.replace('GetFeatureInfo', 'GetTimeSeries').replace('INFO_FORMAT=application%2Fjson', 'INFO_FORMAT=application/csv');

        url = url + '&time=' + start + '/' + end;

        return this.http.get(url)
        .toPromise()
            .then((response)=>{
                let csv =  response.text();
                let rows = csv.split('\n');
                let records = rows.map((r)=>{
                    return r.split(',');
                });

                return records.slice(3, records.length - 1);

                
            })
            .catch((error)=>{
                return Promise.reject(error.message || error);
            });
    }


};
