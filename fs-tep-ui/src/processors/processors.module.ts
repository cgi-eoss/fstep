import {NgModule} from '@angular/core';
import {CommonModule} from '@angular/common';
import {FormsModule} from '@angular/forms';

import {MapModule} from '../map/map.module';
import {CommonComponentsModule} from '../common/common-components.module';

import {ProcessorsRoutingModule} from  './processors-routing.module';
import {ProcessorListComponent} from './processor-list.component/processor-list.component';
import {ProcessorViewComponent} from './processor-view.component/processor-view.component';
import {ProcessorInfoComponent} from './processor-info.component/processor-info.component';
import {ProcessorLegendComponent} from './processor-legend.component/processor-legend.component';
import {TimeseriesPlot} from './timeseries.component/timeseries-plot.component';
import {TimeseriesForm} from './timeseries.component/timeseries-form.component';
import {TimeSeriesWidget} from './timeseries.component/timeseries-widget.component';
import {ProcessorsService} from './processors.service';
import {ProbaVService} from './proba-v.service';

import {ProcessorLayer} from './processor-layer';


@NgModule({
  imports: [
      CommonModule,
      FormsModule,
      ProcessorsRoutingModule,
      MapModule,
      CommonComponentsModule
  ],
  declarations: [
    ProcessorListComponent, ProcessorViewComponent, ProcessorLegendComponent, ProcessorInfoComponent, TimeseriesPlot, TimeseriesForm, TimeSeriesWidget
  ],
  exports: [ProcessorListComponent, ProcessorViewComponent, ProcessorLegendComponent, ProcessorInfoComponent, TimeseriesPlot, TimeseriesForm, TimeSeriesWidget],
  providers: [ProcessorsService, ProcessorLayer, ProbaVService]
})
export class ProcessorsModule {}


//image mosaic rest API

//describe
//http://localhost:9090/geoserver/rest/workspaces/FS-TEP/coveragestores/REFBOA_reproj/coverages/reproj/index.json

//retrieve
//http://localhost:9090/geoserver/rest/workspaces/FS-TEP/coveragestores/REFBOA_reproj/coverages/reproj/index/granules.json?limit=3&filter=location=%27S2A_32UQVSGS_20170713_REFBOA.TIF%27

//by id
//http://localhost:9090/geoserver/rest/workspaces/FS-TEP/coveragestores/REFBOA_reproj/coverages/reproj/index/granules/reproj.1.json

//wms cql
//http://localhost:9090/geoserver/FS-TEP/wms?service=WMS&version=1.1.0&request=GetMap&layers=FS-TEP:refl_543&styles=&bbox=11.714086657726819,48.58834878003898,13.775131037797376,49.645980966056534&width=768&height=394&srs=EPSG:4326&format=application/openlayers&cql_filter=location=%27S2A_32UQVSGS_20170713_REFBOA.TIF%27

//wcs describe
//http://localhost:9090/geoserver/ows?service=WCS&version=2.0.1&request=DescribeCoverage&coverageId=FS-TEP:reproj

//wcs filter
//http://localhost:9090/geoserver/wcs?request=GetCoverage&service=WCS&version=2.0.1&coverageId=FS-TEP:refl_543&cql_filter=location=%27S2A_32UQVSGS_20170713_REFBOA.TIF%27
//https://osgeo-org.atlassian.net/browse/GEOS-8081

//timeseries

//http://cgi-int.eoss-cloud.it/geoserver/fs-prods/wms?SERVICE=WMS&VERSION=1.1.1&REQUEST=GetTimeSeries&FORMAT=image%2Fjpeg&QUERY_LAYERS=fs-prods%3Avito_fapar&INFO_FORMAT=image/jpeg&STYLES&LAYERS=fs-prods%3Avito_fapar&FEATURE_COUNT=50&X=50&Y=50&SRS=EPSG%3A4326&WIDTH=101&HEIGHT=101&BBOX=5.525307655334473%2C51.71131610870361%2C5.5339765548706055%2C51.719985008239746&time=2017-04-01/2017-08-31

//timehistogram
//http://cgi-int.eoss-cloud.it/geoserver/gwc/service/wmts?Request=DescribeDomains&Version=1.0.0&layer=fs-prods:vito_fapar&TileMatrixSet=EPSG:4326

//http://cgi-int.eoss-cloud.it/geoserver/gwc/service/wmts?Request=GetHistogram&Version=1.0.0&BBOX=-90,-180,90,180&time=2017-05-01T00:00:00.000Z/2017-12-01T00:00:00.000Z&histogram=time&resolution=P1M&layer=fs-prods:vito_fapar&TileMatrixSet=EPSG:4326