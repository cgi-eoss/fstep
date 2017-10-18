import { Component, ViewChild, ElementRef, AfterViewInit, HostBinding } from "@angular/core";
import {ResizeSensor} from 'css-element-queries';

import {ProcessorsService} from '../processors.service';
import {ProbaVService} from '../proba-v.service';
import { ProcessorLayer } from '../processor-layer';

@Component({
  selector: 'timeseries-widget',
  templateUrl: './timeseries-widget.component.html',
  styleUrls: ['./timeseries-widget.component.css']
})
export class TimeSeriesWidget implements AfterViewInit {

  public timeseries: any = {
    loading: false
  };

  constructor(private host :ElementRef, private processorService: ProcessorsService, private probaVService: ProbaVService, private processorLayer: ProcessorLayer) {
    this.computeChartSize();
  }

  ngAfterViewInit() {
    let sensor = new ResizeSensor(this.host.nativeElement, (evt)=>{
      this.computeChartSize();
    });
    this.computeChartSize();

    this.processorService.getTimeSeriesState().subscribe((value)=>{
      if (value.enabled)
        setTimeout(()=>this.onFormSubmit(value), 0);
    });
  }

  computeChartSize() {
    this.timeseries.width = this.host.nativeElement.clientWidth - 20;
    this.timeseries.height = this.host.nativeElement.clientHeight - 50;
  }

  closeWidget() {
    this.processorService.setTimeSeriesState({
      enabled: false
    })
  }

  onFormSubmit(data) {

    this.timeseries.loading = true;

    this.processorLayer.getSourceTimeSeries(data.coordinates, data.start.toISOString(), data.end.toISOString()).then((data)=>{
      this.timeseries.data = data.map((item)=>{
        return {
          ts: item[0],
          value: parseFloat(item[1])
        }
      })
      this.timeseries.loading = false;
    }, (error) => {
      console.log(error);

      this.timeseries.loading = false;
    });

    /*
    this.probaVService.getTimeSeries({
      startDate: data.start.toISOString().substr(0, 10),
      endDate: data.end.toISOString().substr(0, 10),
      lat: data.coordinates[1],
      lon: data.coordinates[0],
      productId: 'PROBAV_L3_S10_TOC_NDVI_333M'
    }).subscribe((data)=>{
      this.timeseries.data = data.results.map((item)=>{
        return {
          ts: item.date,
          value: parseFloat(item.result.average) || 0
        }
      });

      this.timeseries.loading = false;
    }, (error) => {
      console.log(error);

      this.timeseries.loading = false;
    });
    */
    

  }
}