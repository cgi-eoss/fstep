import { Injectable } from '@angular/core';

import { BehaviorSubject } from 'rxjs/BehaviorSubject';

import VectorLayer from 'ol/layer/vector';
import VectorSource from 'ol/source/vector';
import ModifyInteraction from 'ol/interaction/modify';
import DrawInteraction from 'ol/interaction/draw';
import GeoJSONParser from 'ol/format/geojson';
import Feature from 'ol/feature';
import Polygon from 'ol/geom/polygon';

import { MapService } from '../map/map.service';


@Injectable()
export class AoiLayer {

    private mapViewer;
    private drawLayer;
    private aoiLayer;
    private geojsonParser;
    private drawInteraction;

    private state = new BehaviorSubject({ visible: false, loading: false, dataSource: null });

    constructor(mapService: MapService) {
        mapService.getViewer().then((viewer) => {
            this.mapViewer = viewer;
            this.initLayer();
        });

        this.geojsonParser = new GeoJSONParser();
    }

    private initLayer() {

        this.aoiLayer = new VectorLayer({
            source: new VectorSource({
            })
        });

        this.mapViewer.addLayer(this.aoiLayer);

        let modifyInteraction = new ModifyInteraction({
            source: this.aoiLayer.getSource(),
            wrapX: true
        });

        this.mapViewer.addInteraction(modifyInteraction);

        this.drawLayer = new VectorLayer({
            source: new VectorSource({
            })
        });


        this.mapViewer.addLayer(this.drawLayer);

        //this.enableDraw('polygon');
    }


    public enableDraw(type) {

        this.drawLayer.getSource().clear();

        if (this.drawInteraction) {
            this.mapViewer.removeInteraction(this.drawInteraction);
        }

        let drawInteraction;
        if (type === 'bbox') {
            drawInteraction = new DrawInteraction({
                source: this.drawLayer.getSource(),
                type: 'Circle',
                geometryFunction: this.bboxGeometryFunction,
                maxPoints: 2,
                wrapX: false
            });
        }
        else {
            drawInteraction = new DrawInteraction({
                source: this.drawLayer.getSource(),
                type: 'Polygon',
                wrapX: false
            });
        }

        drawInteraction.on('drawend', (event) => {
            this.mapViewer.removeInteraction(drawInteraction);

            this.setAoi({
                geometry: JSON.parse(this.geojsonParser.writeGeometry(event.feature.getGeometry(), {
                    dataProjection: 'EPSG:4326',
                    featureProjection: this.mapViewer.getView().getProjection()
                }))
            });

            setTimeout(()=> {
                this.drawLayer.getSource().clear();
            });

        });

        this.mapViewer.addInteraction(drawInteraction);
        this.drawInteraction = drawInteraction;
    };

    setAoi(aoi) {
        this.aoiLayer.getSource().clear();
        if (aoi && aoi.geometry) {
            var geometry = this.geojsonParser.readGeometry(aoi.geometry, {
                dataProjection: 'EPSG:4326',
                featureProjection: this.mapViewer.getView().getProjection()
            });
            var feature = new Feature({
                geometry: geometry
            });
            this.aoiLayer.getSource().addFeature(feature);
        }
    }

    setDrawArea = function (aoi, centerOnMap) {
        this.drawLayer.getSource().clear();
        if (aoi && aoi.geometry) {
            var geometry = this.geojsonParser.readGeometry(aoi.geometry, {
                dataProjection: 'EPSG:4326',
                featureProjection: this.mapViewer.getView().getProjection()
            });
            var feature = new Feature({
                geometry: geometry
            });
            this.drawLayer.getSource().addFeature(feature);

            if (centerOnMap) {
                this.mapService.fitExtent(geometry.getExtent());
            }
        }
    };

    setVisible(visible: boolean) {
        this.state.next(Object.assign({}, this.state.value, {
            visible: visible
        }));
        if (this.aoiLayer) {
            this.aoiLayer.setVisible(visible);
        }
    }

    getCurrentState() {
        return this.state.value;
    }

    getState() {
        return this.state.asObservable();
    }

    private bboxGeometryFunction(coordinates, geometry) {
        if (!geometry) {
            geometry = new Polygon(null);
        }
        let start = coordinates[0];
        let end = coordinates[1];
        geometry.setCoordinates([
            [start, [start[0], end[1]], end, [end[0], start[1]], start]
        ]);
        return geometry;
    };

}