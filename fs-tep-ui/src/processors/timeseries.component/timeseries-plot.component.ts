//https://bl.ocks.org/mbostock/34f08d5e11952a80609169b7917d4172

import { Component, AfterViewInit, OnChanges, Input, ViewChild, ElementRef } from '@angular/core';
import * as d3 from 'd3';


interface SeriesItem {
    ts: string;
    value: number;
}

@Component({
  selector: 'timeseries-plot',
  templateUrl: './timeseries-plot.component.html',
  styleUrls: ['./timeseries-plot.component.scss']
})
export class TimeseriesPlot implements AfterViewInit, OnChanges {
    @Input() width: number;
    @Input() height: number;
    @Input() data: Array<SeriesItem>;
    @Input() title: string;
    @Input() units: string;

    @ViewChild("plot") container: ElementRef;
    @ViewChild("tooltip") tooltip: ElementRef;
    
    private d3_plot_: any = {};
    private transition_: d3.transition;

    ngAfterViewInit() {
        let svg =  d3.select(this.container.nativeElement);

        //let width = this.d3_plot_.attr("width");
        //let height = this.d3_plot_.attr("height");

        let width = this.width;
        let height = this.height;

        let main_position = {
            top: 40,
            left: 50,
            width: width - 50,
            height: height * 0.7 - 70
        }

        let overview_position = {
            top: height * 0.7 + 20,
            left: 50,
            width: width - 50,
            height: height * 0.3 - 20
        }


        svg.append("defs")
            .append("clipPath")
            .attr("id", "clip")
            .append("rect")
            .attr("width", main_position.width)
            .attr("height", main_position.height);


        let x_main = d3.scaleTime().range([0, main_position.width]);
        let y_main = d3.scaleLinear().range([main_position.height, 0]);
        let x_main_axis = d3.axisBottom(x_main);
        let y_main_axis = d3.axisLeft(y_main).ticks(6);


        // let main_area = d3.area()
        //     .curve(d3.curveMonotoneX)
        //     .x((d) => {return x_main(d3.isoParse(d.ts));})
        //     .y0(main_position.height)
        //     .y1((d) => { return y_main(d.value);})

        let main_area = d3.line()
            .curve(d3.curveMonotoneX)
            .x((d) => {return x_main(d3.isoParse(d.ts));})
            .y((d) => { return y_main(d.value);})

        let main_plot = svg.append("g")
            .attr("class", "main")
            .attr("transform", "translate(" + main_position.left + "," + main_position.top + ")");
        
        main_plot.append('path')
            .attr("class", "area")
            //.datum([])
            //.attr("d", main_area);;

        let title = svg.append("text")
            .attr("class", "y-label")
            .attr("text-anchor", "start")
            .attr("x", 10)
            .attr("y", 20);

        title.append("tspan")
            .attr("class", "y-title")
            .html(this.title);

        title.append("tspan")
            .attr("class", "y-units")
            .attr("x", 20)
            .attr("dy", "1.1em")
            .html(this.units);
    
        
        main_plot.append('g')
            .attr("class", "axis axis--x")
            .attr("transform", "translate(0," + main_position.height + ")")
            .call(x_main_axis);
        main_plot.append('g')
            .attr("class", "axis axis--y")
            .call(y_main_axis);

        /*
        main_plot.append('g')
            .attr("class", "grid grid--x")
            .attr("transform", "translate(0," + main_position.height + ")")
            .call(d3.axisBottom(x_main).ticks(5).tickSize(-main_position.height).tickFormat(""));
        */


        let x_overview = d3.scaleTime().range([0, overview_position.width]);
        let y_overview = d3.scaleLinear().range([overview_position.height, 0]);
        let x_overview_axis = d3.axisBottom(x_overview);

        // let overview_area = d3.area()
        //     .curve(d3.curveMonotoneX)
        //     .x((d) => {return x_overview(d3.isoParse(d.ts));})
        //     .y0(overview_position.height)
        //     .y1((d) => { return y_overview(d.value);})

        let overview_area = d3.line()
            .curve(d3.curveMonotoneX)
            .x((d) => {return x_overview(d3.isoParse(d.ts));})
            .y((d) => { return y_overview(d.value);})


        let overview_plot = svg.append("g")
            .attr("class", "overview")
            .attr("transform", "translate(" + overview_position.left + "," + overview_position.top + ")");
            
        overview_plot.append('path')
            .attr("class", "area")
            //.datum([])
            //.attr("d", overview_area);

        overview_plot.append("g")
            .attr("class", "axis axis--x")
            .attr("transform", "translate(0," + (overview_position.height) + ")")
            .call(x_overview_axis);


        let pointer = main_plot.append("g")
            .attr("class", "pointer")
            .style("display", "none");

        let zoom_control = d3.zoom()
            .scaleExtent([1, Infinity])
            .translateExtent([[0, 0], [main_position.width, main_position.height]])
            .extent([[0, 0], [main_position.width, main_position.height]])
            .on("zoom", ()=>{
                if (d3.event.sourceEvent && d3.event.sourceEvent.type === "brush") return; // ignore brush-by-zoom
                let t = d3.event.transform;
                x_main.domain(t.rescaleX(x_overview).domain());
                if (this.data) {
                    main_plot.select(".area").attr("d", main_area);
                    main_plot.selectAll(".dot")
                        .data(this.data)
                        .attr("cx", (d) => { 
                            return x_main(d3.isoParse(d.ts)); 
                        })
                }
                main_plot.select(".axis--x").call(x_main_axis);
                overview_plot.select(".brush").call(brush_control.move, x_main.range().map(t.invertX, t));

                pointer.style("display", "none"); 
            });

        svg.append("rect")
            .attr("class", "zoom")
            .attr("width", main_position.width)
            .attr("height", main_position.height)
            .attr("transform", "translate(" + main_position.left + "," + main_position.top + ")")
            


        let brush_control = d3.brushX()
            .extent([[0, 0], [overview_position.width, overview_position.height]])
            .on("brush end", ()=>{
                if (d3.event.sourceEvent && d3.event.sourceEvent.type === "zoom") return; // ignore brush-by-zoom
                let s = d3.event.selection || x_overview.range();
                x_main.domain(s.map(x_overview.invert, x_overview));
                if (this.data) {
                    main_plot.select(".area").attr("d", main_area);
                    main_plot.selectAll(".dot")
                    .data(this.data)
                    .attr("cx", (d) => { 
                        return x_main(d3.isoParse(d.ts)); 
                    })
                }
                main_plot.select(".axis--x").call(x_main_axis);
                svg.select(".zoom").call(zoom_control.transform, d3.zoomIdentity
                    .scale((this.width - 50) / (s[1] - s[0]))
                    .translate(-s[0], 0));

                pointer.style("display", "none"); 
            });
        
        overview_plot.append("g")
            .attr("class", "brush")
        

        


        pointer.append("circle")
            .attr("r", 4.5);
        
        
        pointer.append("text")
            .attr("x", 9)
            .attr("dy", "-0.5em");

        let bisectDate = d3.bisector(function(d) { return d3.isoParse(d.ts); }).left;

        svg.select('.zoom').on("mouseover", () => {
                if (!this.data || !this.data.length) {
                    return;
                }
                pointer.style("display", null); 
            })
            .on("mouseout", () => { 
                pointer.style("display", "none"); 
            })
            .on("mousemove", () => {
                if (!this.data || !this.data.length) {
                    return;
                }
                pointer.style("display", null); 
                let x0 = x_main.invert(d3.mouse(svg.select('.zoom').node())[0]);
                let i = bisectDate(this.data, x0, 1);
                let d0 = this.data[i - 1];
                let d1 = this.data[i];
                let d = x0 - d3.isoParse(d0.ts) > d3.isoParse(d1.ts) - x0 ? d1 : d0;
                pointer.attr("transform", "translate(" + x_main(d3.isoParse(d.ts)) + "," + y_main(d.value) + ")");

                //this.tooltip.nativeElement.style.top = y_main(d.value);
                //this.tooltip.nativeElement.style.left = x_main(d3.isoParse(d.ts));
                //this.tooltip.nativeElement.text = d.value;
                pointer.select("text").text(d.value.toFixed(2));
            });


        this.d3_plot_ = {
            svg: svg,
            main: {
                x: x_main,
                y: y_main,
                x_axis: x_main_axis,
                y_axis: y_main_axis,
                area: main_area,
                plot: main_plot,
                zoom: zoom_control
            },
            overview: {
                x: x_overview,
                y: y_overview,
                x_axis: x_overview_axis,
                area: overview_area,
                plot: overview_plot,
                brush: brush_control
            }
        }

        this.transition_ = d3.transition()
            .duration(750)
            .ease(d3.easeLinear);
    }

    private createChart() {
        let svg =  d3.select(this.container.nativeElement);

        svg.append("defs")
            .append("clipPath")
            .attr("id", "clip")
            .append("rect")
    }

    private updateChartSize() {

        if (!this.d3_plot_.main) {
            return;
        }

        let width = this.width;
        let height = this.height;

        let main_position = {
            top: 20,
            left: 50,
            width: width - 50,
            height: height * 0.7 - 70
        }

        let overview_position = {
            top: height * 0.7,
            left: 50,
            width: width - 50,
            height: height * 0.3 - 20
        }

        d3.select(this.container.nativeElement).select('#clip rect')
            .attr("width", main_position.width)
            .attr("height", main_position.height);

        this.d3_plot_.main.x.range([0, main_position.width]);
        this.d3_plot_.main.y.range([main_position.height, 0]);

        this.d3_plot_.main.plot.select('g.axis--x')
            .attr("transform", "translate(0," + main_position.height + ")")
            .transition().call(this.d3_plot_.main.x_axis);
        this.d3_plot_.main.plot.select('g.axis--y').transition().call(this.d3_plot_.main.y_axis);

        this.d3_plot_.main.zoom
            .translateExtent([[0, 0], [main_position.width, main_position.height]])
            .extent([[0, 0], [main_position.width, main_position.height]])
        
        this.d3_plot_.svg.select('.zoom')
            .attr("width", main_position.width)
            .attr("height", main_position.height)
            .attr("transform", "translate(" + main_position.left + "," + main_position.top + ")")
            //.call(this.d3_plot_.main.zoom);

        this.d3_plot_.overview.x.range([0, overview_position.width]);
        this.d3_plot_.overview.y.range([overview_position.height, 0]);

        this.d3_plot_.overview.plot.attr("transform", "translate(" + overview_position.left + "," + overview_position.top + ")");
        this.d3_plot_.overview.plot.select('g.axis--x')
            .attr("transform", "translate(0," + overview_position.height + ")")
            .transition().call(this.d3_plot_.overview.x_axis);

        if (this.data) {
            this.d3_plot_.overview.plot.select('path').transition().attr("d", this.d3_plot_.overview.area);
        }
        
        this.d3_plot_.overview.brush
            .extent([[0, 0], [overview_position.width, overview_position.height]]);

        this.d3_plot_.overview.plot.select('.brush')
            .call(this.d3_plot_.overview.brush)
            .call(this.d3_plot_.overview.brush.move, this.d3_plot_.main.x.range());
    }
    

    private drawChart() {

    }

    ngOnChanges(changes: any) {
        if (changes.data) {
            let data = changes.data.currentValue;

            if (data) {

                if (data.length) {
                    let time_extent = d3.extent(data, (d) => {
                        return d3.isoParse(d.ts);
                    });

                   
                    let time_range = (time_extent[1].getTime() - time_extent[0].getTime());
                    this.d3_plot_.main.x.domain([time_extent[0], new Date(time_extent[0].getTime() + time_range * 1.05)]);

                    this.d3_plot_.main.y.domain([0, d3.max(data, (d) => {return d.value;}) * 1.05]);

                    
                    // this.d3_plot_.main.y.domain(d3.extent(data, (d) => {
                    //     return d.value;
                    // }));

                    this.d3_plot_.overview.x.domain(this.d3_plot_.main.x.domain());
                    this.d3_plot_.overview.y.domain(this.d3_plot_.main.y.domain());
                }
                this.d3_plot_.main.plot.select('path').datum(data)
                this.d3_plot_.main.plot.select('path').transition().attr("d", this.d3_plot_.main.area);
                this.d3_plot_.main.plot.select('g.axis--x').transition().call(this.d3_plot_.main.x_axis);
                this.d3_plot_.main.plot.select('g.axis--y').transition().call(this.d3_plot_.main.y_axis);

                let dots = this.d3_plot_.main.plot.selectAll(".dot")
                    .data(data);
                
                dots.exit().remove();

                dots.attr("cx", (d) => { return this.d3_plot_.main.x(d3.isoParse(d.ts)); })
                    .attr("cy", (d) => { return this.d3_plot_.main.y(d.value); });
                
                dots.enter().append("circle")
                    .attr('class', 'dot')
                    .attr("r", 3.5)
                    .attr("cx", (d) => { return this.d3_plot_.main.x(d3.isoParse(d.ts)); })
                    .attr("cy", (d) => { return this.d3_plot_.main.y(d.value); });
                
                
                this.d3_plot_.overview.plot.select('path').datum(data)
                this.d3_plot_.overview.plot.select('path').transition().attr("d", this.d3_plot_.overview.area);
                this.d3_plot_.overview.plot.select('g.axis--x').transition().call(this.d3_plot_.overview.x_axis);

                // this.d3_plot_.main.plot.select('path').transition().duration(750).datum(data).attr("d", this.d3_plot_.main.area);
                // this.d3_plot_.main.plot.select('g.axis--x').transition().duration(750).call(this.d3_plot_.main.x_axis);
                // this.d3_plot_.main.plot.select('g.axis--y').transition().duration(750).call(this.d3_plot_.main.y_axis);

                // this.d3_plot_.overview.plot.select('path').transition().duration(750).datum(data).attr("d", this.d3_plot_.overview.area);
                // this.d3_plot_.overview.plot.select('g.axis--x').transition().duration(750).call(this.d3_plot_.overview.x_axis);

                this.d3_plot_.overview.plot.select('.brush')
                    .call(this.d3_plot_.overview.brush)
                    .call(this.d3_plot_.overview.brush.move, this.d3_plot_.main.x.range());

                this.d3_plot_.overview.plot.select('.brush rect.selection').attr('rx', 5).attr('ry', 5);
                this.d3_plot_.svg.select('.zoom')
                    .call(this.d3_plot_.main.zoom);
                    

            }
        }
        if (changes.width || changes.height) {
            this.updateChartSize();
        }
        if (changes.title) {
            if (this.d3_plot_.svg)
                this.d3_plot_.svg.select('.y-title').html(changes.title.currentValue);
        }
        if (changes.units) {
            if (this.d3_plot_.svg)
                this.d3_plot_.svg.select('.y-units').html(changes.units.currentValue);
        }
    }
};