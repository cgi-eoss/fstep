import { Component, EventEmitter, AfterViewInit, ViewChild, ElementRef, HostBinding } from "@angular/core";
import { trigger, state, style, animate, group, stagger, query, transition } from '@angular/animations';
import { Router } from '@angular/router';

import { ResizeSensor } from 'css-element-queries';

import { ProcessorsService } from '../processors.service';

import * as GeminiScrollbar from 'gemini-scrollbar';

@Component({
  selector: 'processor-list',
  templateUrl: './processor-list.component.html',
  styleUrls: ['./processor-list.component.scss'],
  animations: [
    trigger('fadeIn', [
      transition('* => *', [
        // query(':enter', [
        //   style({ transform: 'translateX(-100vh) rotateZ(90deg)', opacity: 0}),
        //   stagger(200, [
        //     animate('0.5s ease-out', style({ transform: 'translateX(0px) rotateZ(0deg)', opacity: 1 }))
        //   ])
        // ], { optional: true },)
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
        //style({opacity: '1'}),
        //animate('1s ease-out', style({opacity: '0'}))
        group([
          query('.card.is-active', [
            style({ transform: 'scale(1)', opacity: '1', 'tranform-origin': 'center' }),
            animate('1s ease-out', style({ transform: 'scale(2)', opacity: '0' }))
          ]),
          query('.card:not(.is-active)', [
            style({ transform: 'scale(1)', opacity: '1' }),
            animate('1s ease-out', style({ transform: 'scale(0)', opacity: '0' }))
          ])
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
export class ProcessorListComponent implements AfterViewInit {
  @HostBinding('@routeAnimation') routeAnimation = true;
  //@HostBinding('style.display')   display = 'block';
  //@HostBinding('style.position')  position = 'absolute';
  @ViewChild("container") container: ElementRef;
  public processors = [];

  constructor(private processorsService: ProcessorsService, private router: Router) {

    processorsService.setActiveProcessor(null);
    processorsService.getProcessorsList().then((data) => {
      Array.prototype.push.apply(this.processors, data);
    });
  }

  ngAfterViewInit() {
  }

  onProcessorSelect(processor) {
    this.processorsService.setActiveProcessor(processor);
    setTimeout(() => this.router.navigate(['/processor', processor.id]), 0);
  }

}