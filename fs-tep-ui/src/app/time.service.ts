import  {Injectable} from '@angular/core';
import {BehaviorSubject} from 'rxjs/BehaviorSubject';
import {Subject} from 'rxjs/Subject';


@Injectable()
export class TimeService {

    private selectedDate: BehaviorSubject<Date> = new BehaviorSubject(new Date());
    private dateConstraints: BehaviorSubject<{minDate: Date, maxDate: Date, dateList: Array<Date>}> = new BehaviorSubject({
        minDate: null,
        maxDate: null,
        dateList: null
    })

    constructor() {
        
    }

    public setSelectedDate(dt: Date) {
        this.selectedDate.next(dt);
    }

    public getSelectedDate() {
      return this.selectedDate.asObservable();
    }

    public getCurrentDate() {
        return this.selectedDate.value;
    }

    setDateContraints(constraints) {
        this.dateConstraints.next(constraints);
    }

    getDateConstraintsObs() {
        return this.dateConstraints.asObservable();
    }

    getDateConstraints() {
        return this.dateConstraints.value;
    }


};

