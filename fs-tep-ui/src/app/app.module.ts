import {NgModule} from '@angular/core';
import {BrowserModule} from '@angular/platform-browser';
import {BrowserAnimationsModule} from '@angular/platform-browser/animations';

import {HttpModule} from '@angular/http';
import {FormsModule} from '@angular/forms';

import {AppRoutingModule} from './app-routing.module';
import {MapModule} from '../map/map.module';
import {CommonComponentsModule} from '../common/common-components.module';

import {AppComponent} from './app.component/app.component';
import {NavbarComponent} from './navbar.component/navbar.component';
import {StatusbarComponent} from './statusbar.component/statusbar.component';
import {TimeSelectorComponent} from './timeselector.component/timeselector.component';

import {UserService} from './user/user.service';
import {TimeService} from './time.service';

import {ProcessorsModule} from '../processors/processors.module';

@NgModule({
  declarations: [AppComponent, NavbarComponent, StatusbarComponent, TimeSelectorComponent],
  imports: [BrowserModule, HttpModule, BrowserAnimationsModule, MapModule, CommonComponentsModule, ProcessorsModule, AppRoutingModule],
  providers: [UserService, TimeService],
  bootstrap: [AppComponent]
})
export class AppModule { }