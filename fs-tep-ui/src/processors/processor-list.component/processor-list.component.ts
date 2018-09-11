import { Component, EventEmitter, OnInit, ViewChild, ElementRef, HostBinding } from "@angular/core";
import { trigger, state, style, animate, group, stagger, query, transition } from '@angular/animations';
import { Router, ActivatedRoute, ParamMap } from '@angular/router';

import { ResizeSensor } from 'css-element-queries';

import { ProcessorsService } from '../processors.service';
import {Processor} from '../processor';

import * as GeminiScrollbar from 'gemini-scrollbar';

@Component({
  selector: 'processor-list',
  templateUrl: './processor-list.component.html',
  styleUrls: ['./processor-list.component.scss'],
  animations: [
    trigger('fadeIn', [
      transition('* => *', [
        query(':enter', [
          style({ opacity: 0, transform: 'translateX(-100%) rotateZ(-90deg)' }),
          stagger(200, [
            animate('0.5s ease-out', style({ transform: 'translateX(0px) rotateZ(0deg)', opacity: 1 }))
          ])
        ], { optional: true }, )
      ]),
      transition(':leave', [
        style({ transform: 'translateX(0px) rotateX(0deg)' }),
        animate('1s ease-out', style({ transform: 'translateX(-100%) rotateX(-90deg)' })),
      ])
    ]),
    trigger('routeAnimation', [
      transition(':leave', [
        group([
          query('.card.is-active', [
            style({ transform: 'scale(1)', opacity: '1', 'tranform-origin': 'center' }),
            animate('1s ease-out', style({ transform: 'scale(2)', opacity: '0' }))
          ], { optional: true }),
          query('.card:not(.is-active)', [
            style({ transform: 'scale(1)', opacity: '1' }),
            animate('1s ease-out', style({ transform: 'scale(0)', opacity: '0' }))
          ], { optional: true })
        ])
      ]),

      transition(':enter', [
        query('.card', [
          style({ opacity: 0, transform: 'translateX(-100%) rotateZ(-90deg)' }),
          stagger(200, [
            animate('0.5s ease-out', style({ transform: 'translateX(0px) rotateZ(0deg)', opacity: 1 }))
          ])
        ], { optional: true })
      ])

    ])
  ]
})
export class ProcessorListComponent implements OnInit {
  @HostBinding('@routeAnimation') routeAnimation = true;
  @ViewChild("container") container: ElementRef;
  public service: string;
  public processors = [];

  constructor(
    private processorsService: ProcessorsService, 
    private router: Router,
    private route: ActivatedRoute
  ) {

  }

  ngOnInit() {
    this.route.paramMap
      .switchMap((params: ParamMap) => {
        this.service = params.get('service');
        return this.processorsService.getProcessorsList(params.get('service'));
      }).subscribe((processorList: Processor[]) => {
        this.processors.length = 0;
        Array.prototype.push.apply(this.processors, processorList);
        this.processorsService.setActiveProcessor(null);
      });
  }

  onProcessorSelect(processor) {
    this.processorsService.setActiveProcessor(processor);
    setTimeout(() => this.router.navigate(['/products', this.service, processor.id]), 0);
  }

}