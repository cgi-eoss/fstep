import {Component, Input, ElementRef, OnChanges, HostBinding} from "@angular/core";


@Component({
  selector: 'loader-overlay',
  templateUrl: './loader-overlay.component.html',
  styleUrls: ['./loader-overlay.component.css']
})
export class LoaderOverlay {
  @HostBinding('class.visible') @Input() visible: boolean = false;


  constructor(private elRef:ElementRef) {
    
  }

}