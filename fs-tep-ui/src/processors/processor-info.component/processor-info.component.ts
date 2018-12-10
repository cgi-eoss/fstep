import { Component, AfterViewInit, OnDestroy, ViewChild, ElementRef, Input } from "@angular/core";

import Overlay from 'ol/overlay';
import Proj from 'ol/proj';

import { MapService } from '../../map/map.service';
import {TimeService} from '../../app/time.service';

import {ProcessorsService} from '../processors.service'
import { ProcessorLayer } from '../processor-layer';

@Component({
    selector: 'processor-info',
    templateUrl: './processor-info.component.html',
    styleUrls: ['./processor-info.component.scss']
})
export class ProcessorInfoComponent implements AfterViewInit, OnDestroy {
    @ViewChild("container") container: ElementRef;
    private popup;
    private viewer;
    public position = [0,0];
    public showTimeseriesButton;

    public data = {
        status: 'empty',
        value: 0
    }

    constructor(
        private mapService: MapService, 
        private processorLayer: ProcessorLayer, 
        private processorService: ProcessorsService,
        private timeService: TimeService
    ) {
        processorService.getTimeSeriesState().subscribe((value)=>{
            this.position = value.coordinates;
            this.showTimeseriesButton = !value.enabled && this.processorService.getActiveProcessor().hasTimeDimension();
        });
        

    }

    ngAfterViewInit() {

        let overlay = new Overlay({
            element: this.container.nativeElement,
            autoPan: true,
            autoPanAnimation: {
              duration: 250
            },
            positioning: 'bottom-left'
        });

        this.mapService.getViewer().then((viewer) => {

            this.viewer = viewer;

            viewer.addOverlay(overlay);
            viewer.on('singleclick', this.onMapClick, this);

            this.container.nativeElement.addEventListener('mousedown', (evt) => {
                let down_coord = viewer.getEventCoordinate(evt);
                let overlay_coord  = overlay.getPosition();
                let offset = [overlay_coord[0] - down_coord[0], overlay_coord[1] - down_coord[1]];

                let that = this;
                let moved = false;
                function move(evt) {
                    let coord = viewer.getEventCoordinate(evt);
                    
                    if (down_coord[0] == coord[0] && down_coord[1] == coord[1]) {
                        return;
                    }
                    that.position = [coord[0] + offset[0], coord[1] + offset[1]]
                    overlay.setPosition(that.position);
                    that.data = {
                        status: 'empty',
                        value: 0
                    }
                    moved = true;
                }
                function end(evt) {
                  window.removeEventListener('mousemove', move);
                  window.removeEventListener('mouseup', end);
                  if (moved)
                    that.retrieveDataValue(that.position);
                }
                window.addEventListener('mousemove', move);
                window.addEventListener('mouseup', end);
            });
        });

        this.popup = overlay;

        this.timeService.getSelectedDate().subscribe((dt)=>{
            if (this.popup.getPosition() != null) {
                this.retrieveDataValue(this.position);
            }
        });


    }

    getGeoCoordinates(coord) {
        if (!this.viewer) {
            return null;
        }
        if (this.viewer.getView().getProjection() != 'EPSG:4326') {
            coord = Proj.transform(coord, this.viewer.getView().getProjection(), 'EPSG:4326');
        }
        return coord;
    }

    retrieveDataValue(coordinate) {

        this.data = {
            status: 'loading',
            value: 0
        }
        this.processorLayer.getSourceValue(coordinate).then((value)=>{
            this.data = {
                status: 'ready',
                value: value
            }
        });

        this.processorService.setTimeSeriesState({
            coordinates: coordinate
        });
    }

    onTimeSeriesClick() {
        this.processorService.setTimeSeriesState({
            enabled: true
        });
    }

    closePopup() {
        this.popup.setPosition(null);
        this.processorService.setTimeSeriesState({
            enabled: false
        });
    }

    ngOnDestroy() {
        this.mapService.getViewer().then((viewer)=>{
            viewer.removeOverlay(this.popup);
            viewer.un('singleclick', this.onMapClick, this);
        });
    }

    private onMapClick(evt) {
        let coordinate = evt.coordinate;
        this.position = coordinate;
        this.popup.setPosition(this.position);
        this.retrieveDataValue(coordinate);
    }
}