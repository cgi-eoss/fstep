import { Component, ViewChild, ElementRef, AfterViewInit} from "@angular/core";

import { TimeService } from "../time.service";

import * as flatpickr from 'flatpickr';
import * as moment from 'moment';

@Component({
    selector: 'time-selector',
    templateUrl: './timeselector.component.html',
    styleUrls: ['./timeselector.component.scss']
})
export class TimeSelectorComponent implements AfterViewInit {
    @ViewChild('datePickerTarget') pickerInput : ElementRef;
    public ts: string;
    private flatPickerInstance;

    constructor(private timeService: TimeService) {
        this.timeService.getSelectedDate().subscribe((value) => {
            this.onTimeChange(value);
        })
    }

    ngAfterViewInit() {
        this.flatPickerInstance = flatpickr(this.pickerInput.nativeElement, {
            onChange: (value) => {
                if (value && value.length)
                    this.timeService.setSelectedDate(value[0]);
            },
            defaultDate: this.timeService.getCurrentDate(),
            disableMobile: "true"
        });

        this.timeService.getDateConstraintsObs().subscribe((constraints) => {
            if(!constraints) {
                this.resetConstraints();
            }
            else {
                if (constraints.dateList) {
                    this.setSelectableDates(constraints.dateList);
                }
                if (constraints.minDate || constraints.maxDate) {
                    this.setSelectableRange(constraints.minDate, constraints.maxDate);
                }
                
            }
        });

    }

    private onTimeChange(dt: Date) {
        this.ts = moment(dt).format('YYYY-MM-DD');
        if (this.flatPickerInstance) {
            this.flatPickerInstance.jumpToDate(dt);
            this.flatPickerInstance.setDate(dt);
        }
    }

    selectDate() {
        this.flatPickerInstance.open();
    }

    private resetConstraints() {
        this.flatPickerInstance.set('enable', []);
        this.flatPickerInstance.set('minDate', null);
        this.flatPickerInstance.set('maxDate', null);
    }

    private setSelectableRange(start: Date, end: Date) {
        //this.flatPickerInstance.set('enable', []);
        this.flatPickerInstance.set('minDate', start);
        this.flatPickerInstance.set('maxDate', end);
    }

    private setSelectableDates(dates: Array<Date>) {
        //this.flatPickerInstance.set('minDate', null);
        //this.flatPickerInstance.set('maxDate', null);
        this.flatPickerInstance.set('enable', dates);
    }
};
