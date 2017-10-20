import { Component, Output, EventEmitter, ViewChild, ElementRef, AfterViewInit } from "@angular/core";

import {ProcessorsService} from '../processors.service';

import * as flatpickr from 'flatpickr';

@Component({
    selector: 'timeseries-form',
    templateUrl: './timeseries-form.component.html',
    styleUrls: ['./timeseries-form.component.css']
})
export class TimeseriesForm implements AfterViewInit{
    @ViewChild('timeRangeField') timeRangeInput: ElementRef;

    @Output() formsubmit: EventEmitter<any> = new EventEmitter();

    constructor(private processorService: ProcessorsService) {
        
    }

    ngAfterViewInit() {
        let rangePicker = flatpickr(this.timeRangeInput.nativeElement, {
            onChange: (value)=> {
                if (value.length == 2) {
                    this.processorService.setTimeSeriesState({
                        start: value[0],
                        end: value[1]
                    });
                }
            },
            mode: 'range'
        });

        let val = this.processorService.getTimeSeriesStateValue();

        setTimeout(()=>{
            rangePicker.setDate([val.start, val.end]);
        }, 0)

        this.processorService.getTimeSeriesState().subscribe((value)=>{
            rangePicker.setDate([value.start, value.end], false);
        });
        
        this.processorService.getActiveProcessorObs().subscribe((processor)=>{
            if (processor) {
                rangePicker.set('minDate', processor.timeRange.start.toDate());
                rangePicker.set('maxDate', processor.timeRange.end.toDate());
            }
            
        })
    
    }

    onSubmitClick() {
        /*
        this.formsubmit.emit({
            start: this.startDateInput.nativeElement._flatpickr.selectedDates[0],
            end: this.endDateInput.nativeElement._flatpickr.selectedDates[0]
        });
        */
    }
}