import { Pipe, PipeTransform } from '@angular/core';
import * as dms from 'geodesy/dms';

@Pipe({name: 'latitude'})
export class LatitudePipe implements PipeTransform {
  transform(value: number, format: string, options: any = {}): string {
    if (format == 'dms') {
        return dms.toLat(value)
    }
    else {
        return value.toFixed(options.precision || 3);
    }
  }
}