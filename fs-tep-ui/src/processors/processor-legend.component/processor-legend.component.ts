import { Component, AfterViewInit, OnDestroy, ViewChild, ElementRef, Input } from "@angular/core";

import { MapService } from '../../map/map.service';
import { UserService} from '../../app/user/user.service';
import { ProcessorLayer } from '../processor-layer';
import { ColorMap} from '../colormap';

import { select, Selection } from 'd3-selection';
import { scaleLinear } from 'd3-scale';
import { axisRight, axisBottom, axisTop, axisLeft } from 'd3-axis';
import 'd3-transition';

import * as inside from '@turf/inside';
import * as helpers from '@turf/helpers';


@Component({
    selector: 'processor-legend',
    templateUrl: './processor-legend.component.html',
    styleUrls: ['./processor-legend.component.scss']
})
export class ProcessorLegendComponent implements AfterViewInit, OnDestroy {
    @ViewChild("svg") svg: ElementRef;
    @Input() width: number;
    @Input() height: number;
    @Input() layout;

    private layerObserver;
    private dataSource = null;
    private dataCursor;
    private colorToDomain;
    private userArea;
    public hasColormap = false;
    public legendUrl;

    constructor(private processorLayer: ProcessorLayer, private mapService: MapService, userService: UserService) {
        userService.getUser().subscribe((user)=>{
            this.updateUserArea(user);
        });

    }

    ngAfterViewInit() {
        this.layerObserver = this.processorLayer.getState().subscribe((state) => {
            this.dataSource = state.dataSource;
            this.hasColormap = this.createColorGradient();
            if (this.hasColormap)
                this.colorToDomain = this.dataSource.getColorMap().getInverseMap();
            this.legendUrl = this.dataSource ? this.dataSource.getLegendUrl() : null;
        });

        let w = this.width;

        this.mapService.getViewer().then((viewer) => {
            viewer.on('pointermove', (evt) => {

                viewer.getTargetElement().style.cursor = '';

                if (!this.hasColormap) {
                    return;
                }
                
                if (evt.dragging) {
                    this.dataCursor.transition().attr('opacity', '0');
                    return;
                }

                var pixel = viewer.getEventPixel(evt.originalEvent);

                if (this.userArea) {
                    let coord = viewer.getCoordinateFromPixel(pixel);
                    let pt = helpers.point(coord);
                    if (!inside(pt, this.userArea)) {
                        this.dataCursor.transition().attr('opacity', '0');
                        return;
                    }
                }

                var hit = viewer.forEachLayerAtPixel(pixel, (layer, rgba) => {
                    if (rgba && rgba[3] != 0) {
                        let val = this.colorToDomain[ColorMap.rgbToHex(rgba)];
                        if (val) {
                            let pos = val / 255 * w;
                            
                            this.dataCursor.transition().attr('y', pos - 3).attr('opacity', '1');
                        }

                        return true;
                    }
                    else {
                        this.dataCursor.transition().attr('opacity', '0');
                    }
                }, this, (layer) => {
                    if (layer.get('id') != 'base') {
                        return true;
                    }
                    return false;
                });
                viewer.getTargetElement().style.cursor = hit ? 'pointer' : '';
            });
        });
    }

    ngOnDestroy() {
        this.layerObserver.unsubscribe();
    }

    private updateUserArea(user) {
        if (user) {
            let coords = user.subscription.area;
            if (coords) {
                this.userArea = helpers.multiPolygon(coords);
            }
        }

    }

    createColorGradient() {
        if (this.dataSource) {
            let colormap = this.dataSource.getColorMap();
            if (colormap) {

                setTimeout(()=>{

                let steps = colormap.getColorSteps();

                let defs = select(this.svg.nativeElement).select('defs');
 
                defs.selectAll("linearGradient").remove();
                select(this.svg.nativeElement).selectAll("g").remove();

                let stops = defs.append("linearGradient")
                    .attr("id", "colormap-gradient")
                    .attr("x1", "0%").attr("y1", "0%")
                    .attr("x2", "0%").attr("y2", "100%")
                    .selectAll("stop")
                    .data(steps);

                stops.enter().append("stop")
                    .attr("offset", function (d, i) { return i / (steps.length - 1); })
                    .attr("stop-color", function (d) { return d; });

                let legendsvg = select(this.svg.nativeElement).append("g");

                legendsvg.append("rect")
                    .attr("class", "legendRect")
                    .attr("width", this.height)
                    .attr("height", this.width)
                    .style("fill", "url(#colormap-gradient)");

                let xScale = scaleLinear()
                    .range([0, this.width])
                    .domain(this.dataSource.getDomain());

                let xAxis = axisRight(xScale)
                    .ticks(5)  //Set rough # of ticks

                legendsvg.append("g")
                    .attr("class", "axis")  //Assign "axis" class
                    .attr("transform", "translate(" + this.height + ",0)")
                    .call(xAxis);

                let cursor = legendsvg.append("rect")
                    .attr('class', 'cursor')
                    .attr('width', this.height + 6)
                    .attr('x', -3)
                    .attr('height', 5)
                    .attr('rx', 3)
                    .attr('ry', 3)
                    .attr('opacity', 0)

                this.dataCursor = cursor;

                let bbox =  this.svg.nativeElement.getBBox();
                let padding = 0;
                this.svg.nativeElement.setAttribute("viewBox", (bbox.x-padding)+" "+(bbox.y-padding)+" "+(bbox.width+padding*2)+" "+(bbox.height+padding*2));
                this.svg.nativeElement.setAttribute("width", (bbox.width+padding*2)  + "px");
                this.svg.nativeElement.setAttribute("height",(bbox.height+padding*2) + "px");
                });
                
                return true;
            }


        }

        return false;
    }


}