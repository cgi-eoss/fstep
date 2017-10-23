import {Component} from "@angular/core";

import {CoordService} from "../coord.service";

@Component({
  selector: 'mouse-coords',
  templateUrl: './mouse-coords.component.html',
  styleUrls: ['./mouse-coords.component.css']
})
export class MouseCoordsComponent {

  public coords: {lat: number, lon: number} = null;
  public format: string = 'dms';
  
  constructor(coordService: CoordService) {
    coordService.getMouseCoords().subscribe((value)=> {
      this.coords = value;
    })
  }
};
