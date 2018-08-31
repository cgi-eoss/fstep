import {Component, OnInit, OnDestroy, HostBinding} from "@angular/core";
import {Router, ActivatedRoute, ParamMap} from "@angular/router";
import 'rxjs/add/operator/switchMap';

import {trigger, state, style, animate, group, stagger, query, transition} from '@angular/animations';

import {ProcessorsService} from '../processors.service';
import {ProcessorLayer} from '../processor-layer';
import {Processor} from '../processor';
import {MapDrawControl} from '../../map/map-draw-control';

@Component({
  selector: 'processor-view',
  templateUrl: './processor-view.component.html',
  styleUrls: ['./processor-view.component.scss'],
  animations: [
    trigger('slideIn', [
      transition(':enter', [
          style({ transform: 'scaleX(0)', opacity: 0}),
          animate('0.5s ease-in', style({ transform: 'scaleX(1)', opacity: 1}))
      ]),
      transition(':leave', [
        style({ transform: 'scaleX(1)', opacity: 1}),
        animate('0.5s ease-in', style({ transform: 'scaleX(0)', opacity: 0}))
      ])
    ])
  ]
})
export class ProcessorViewComponent implements OnInit, OnDestroy {

  public showTimeSeries = false;
  private processorSubs;

  constructor(
    private processorLayer: ProcessorLayer, 
    private processorsService: ProcessorsService,
    private route: ActivatedRoute,
    private router: Router
  ) {
    
       
  }

  ngOnInit() {
    this.route.paramMap
      .switchMap((params: ParamMap) => {
        return this.processorsService.getProcessor(params.get('service'), params.get('id'));
      }).subscribe((processor: Processor) => {
        this.processorLayer.setDataSource(processor.mapSource);
        this.processorLayer.setVisible(true);
        this.processorsService.setActiveProcessor(processor);
      });

    this.processorSubs = this.processorsService.getTimeSeriesState().subscribe((value)=>{
      this.showTimeSeries = value.enabled;
    });
  }

  ngOnDestroy() {
    this.processorLayer.setVisible(false);
    this.processorSubs.unsubscribe();
  }

}