import {Injectable} from '@angular/core';
import {Http, Response, URLSearchParams} from '@angular/http';

import {BehaviorSubject} from 'rxjs/BehaviorSubject';

import {MapService} from '../map/map.service';
import {UserService} from '../app/user/user.service';
import {TimeService} from '../app/time.service';
import {WmtsDomainDiscoveryService} from '../map/services/wmts-domain-discovery.service';

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

    constructor(
        private mapService: MapService, 
        userService: UserService,
        private wmtsDomainDiscoveryService: WmtsDomainDiscoveryService,
        private timeService: TimeService, 
        private http: Http
    ) {
        mapService.getViewer().then((viewer) => {
            this.viewer = viewer;
            this.initLayer();
        });

        userService.getUser().subscribe((user)=>{
            this.updateUserArea(user);
        });

        timeService.getSelectedDate().subscribe((dt)=>{
            let ds = this.state.value.dataSource;
            if (ds && ds.hasTimeDimension()) {
                ds.updateSourceTime(dt);
                this.centerOnMap();
            }
        })
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

        if (source && source.hasTimeDimension()) {
            source.updateSourceTime(this.timeService.getCurrentDate());
            this.centerOnMap();
        }

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

        let dataSource = this.state.value.dataSource;
        let url = dataSource.getFeatureInfo(coordinate, this.viewer.getView().getResolution(), this.viewer.getView().getProjection());

        return this.http.get(url)
            .toPromise()
            .then((response)=>{
                let features =  response.json().features;
                if (features.length) {
                    let val =  dataSource.scaleValue(features[0].properties.GRAY_INDEX);
                    if (typeof(val) === "number") {
                        return val.toFixed(2);
                    } else {
                        return val;
                    }
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

        let dataSource = this.state.value.dataSource;

        let url = dataSource.getFeatureInfo(coordinate, this.viewer.getView().getResolution(), this.viewer.getView().getProjection());

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

                return records.slice(3, records.length - 1).map((value) => {
                    return [value[0], dataSource.scaleValue(parseFloat(value[1]))];
                }).filter((value) => {
                    return typeof(value[1]) == "number";
                });

                
            })
            .catch((error)=>{
                return Promise.reject(error.message || error);
            });
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

        this.viewer.getLayers().push(this.layer);

        this.setVisible(this.state.value.visible);

        this.onUserAreaUpdated();

    }

    private updateUserArea(user) {
        if (user) {
            let coords = user.subscription.area;

            if (coords) {
                this.clipGeom = new MultiPolygon(coords);
            }
        }

        this.onUserAreaUpdated();

    }

    private onUserAreaUpdated() {
        if (this.layer) {

            if (this.clipGeom) {
                this.layer.setExtent(this.clipGeom.getExtent());
                
                this.layer.on('precompose', this.onLayerPrecompose, this);
                this.layer.on('postcompose', this.onLayerPostCompose, this);
            }
            else {

                this.layer.setExtent();

                this.layer.un('precompose', this.onLayerPrecompose, this);
                this.layer.un('postcompose', this.onLayerPostCompose, this);
            }
        }
    }

    private onLayerPrecompose(event) {
        
        let fillStyle = new FillStyle({color: [0, 0, 0, 0]});

        var ctx = event.context;
        var vecCtx = event.vectorContext;
        ctx.save();
        // Using a style is a hack to workaround a limitation in
        // OpenLayers 3, where a geometry will not be draw if no
        // style has been provided.
        vecCtx.setFillStrokeStyle(fillStyle, null);
        vecCtx.drawGeometry(this.clipGeom);
        ctx.clip();
    }

    private onLayerPostCompose(event) {
        var ctx = event.context;
        ctx.restore();
    }

    private centerOnMap() {

        if (!this.layer) {
            return;
        }

        let wmsSource = this.layer.getSource()
        let url = wmsSource.getUrls()[0].replace('wms', 'gwc/service/wmts');
        let params = wmsSource.getParams();

        this.wmtsDomainDiscoveryService.describeDomains({
            url: url,
            layer: params['LAYERS'],
            tileMatrix: 'EPSG:4326',
            restrictions: [{
                dimension: 'time',
                range: params['TIME']
            }]
        }).then((domains) => {
            if (domains && domains.bbox) {
                this.mapService.fitExtent([domains.bbox.minx, domains.bbox.miny, domains.bbox.maxx, domains.bbox.maxy])
            }
        });
    }



};
