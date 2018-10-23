/**
 * @ngdoc service
 * @name fstepApp.SearchService
 * @description
 * # SearchService
 * Service in the fstepApp.
 */
define(['../fstepmodules', 'traversonHal', 'moment'], function (fstepmodules, TraversonJsonHalAdapter, moment) {

    'use strict';

    fstepmodules.service('SearchService', ['fstepProperties', '$http', '$q', 'MessageService', 'traverson', function (fstepProperties, $http, $q, MessageService, traverson) {

        var _this = this;
        traverson.registerMediaType(TraversonJsonHalAdapter.mediaType, TraversonJsonHalAdapter);
        var rootUri = fstepProperties.URLv2;
        var halAPI =  traverson.from(rootUri).jsonHal().useAngularHttp();

        this.spinner = { loading: false };

        this.params = {
            pagingData: {},
            results: {},
            searchParameters: {}
        };

        var updateSearchParametersFromResponse = function(response) {
            var params = response.parameters.parameters;
            for (var key in params) {
                params[key] = params[key][0];
            }
            _this.params.searchParameters = params;
        };

        /* Get Groups for share functionality to fill the combobox */
        this.getSearchParameters = function(params){

            params = params || {}
            let url = rootUri + '/search/parameters';
            var deferred = $q.defer();
            halAPI.from(url)
                .newRequest()
                .withRequestOptions({ qs: {
                    resolveAll: params.noAutoResolve ? 'false' : 'true' }
                })
                .getResource()
                .result
                .then(
                function (document) {
                    deferred.resolve(document);
                }, function (error) {
                    MessageService.addError('Could not get Search Data', error);
                    deferred.reject();
                });
            return deferred.promise;
        };

        this.resolveSearchParameter = function(param){

            let url = rootUri + '/search/parameter/' + param;

            var deferred = $q.defer();
            halAPI.from(url)
                .newRequest()
                .getResource()
                .result
                .then(
                function (document) {
                    deferred.resolve(document);
                }, function (error) {
                    MessageService.addError('Could not get Search Data', error);
                    deferred.reject();
                });
            return deferred.promise;
        }

        /* Get search name to display in the bottombar tab */
        this.getSearchName = function() {
            if(this.params.searchParameters.mission) {
                return ': ' + this.params.searchParameters.mission;
            } else {
                return '';
            }
        };

        /* Get results by page */
        this.getResultsPage = function (url) {
            this.spinner.loading = true;
            var deferred = $q.defer();

            halAPI.from(url)
                .newRequest()
                .getResource()
                .result
                .then(
                function (response) {
                    _this.spinner.loading = false;
                    deferred.resolve(response);
                }, function (error) {
                    _this.spinner.loading = false;
                    MessageService.addError('Search failed', error);
                    deferred.reject();
                });

            return deferred.promise;
        };

        /* Submit search and get results */
        this.submit = function(searchParameters){
            this.spinner.loading = true;
            var deferred = $q.defer();

            halAPI.from(rootUri + '/search')
                .newRequest()
                .withRequestOptions({ qs: searchParameters })
                .getResource()
                .result
                .then(
                function (response) {
                    _this.spinner.loading = false;
                    updateSearchParametersFromResponse(response);
                    deferred.resolve(response);
                }, function (error) {
                    _this.spinner.loading = false;
                    MessageService.addError('Search failed', error);
                    deferred.reject();
                });
            return deferred.promise;
        };

        return this;
    }]);
});
