import { Pipe, PipeTransform } from '@angular/core';
import * as dms from 'geodesy/dms';

@Pipe({name: 'longitude'})
export class LongitudePipe implements PipeTransform {
  transform(value: number, format: string, options: any = {}): string {
    if (format == 'dms') {
        return dms.toLon(value)
    }
    else {
        return value.toFixed(options.precision || 3);
    }
  }
}
