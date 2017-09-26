import {Component, AfterViewInit, ViewChild, ElementRef} from "@angular/core";

import {MapService} from './map.service';

@Component({
  selector: 'map-widget',
  template: '<div #map_widget class="map_widget"></div>',
  styles: [ 
    `.map_widget {
        width: 100%;
        height: 100%;
    }`
  ]
})
export class MapComponent implements  AfterViewInit {
    @ViewChild("map_widget") container: ElementRef;

    constructor(private map: MapService) {}

    ngAfterViewInit() {
        this.map.initMap(this.container.nativeElement);
    }
};