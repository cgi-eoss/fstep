/**
 * @ngdoc service
 * @name fstepApp.BasketService
 * @description
 * # BasketService
 * Service in the fstepApp.
 */
'use strict';

define(['../fstepmodules', 'traversonHal'], function(fstepmodules, TraversonJsonHalAdapter) {

    fstepmodules.service('UserEndpointService', ['$rootScope', '$http', 'fstepProperties', '$q', '$timeout', 'MessageService', 'UserService', 'traverson', function($rootScope, $http, fstepProperties, $q, $timeout, MessageService, UserService, traverson) {

        var self = this;

        traverson.registerMediaType(TraversonJsonHalAdapter.mediaType, TraversonJsonHalAdapter);
        var rootUri = fstepProperties.URLv2;
        var halAPI = traverson.from(rootUri).jsonHal().useAngularHttp();
        var deleteAPI = traverson.from(rootUri).useAngularHttp();

        this.endpoints = [];

        $rootScope.$on('active.user', function(event, user) {
            self.getUserEndpoints().then(function(endpoints) {
                self.endpoints = endpoints;
            });
        })

        this.getUserEndpoints = function() {
            var deferred = $q.defer();
            halAPI.from(rootUri + '/userEndpoints/')
                .newRequest()
                .getResource()
                .result
                .then(
                function (response) {
                    deferred.resolve(response._embedded.userEndpoints);
                }, function (error) {
                    MessageService.addError('Get user endpoints failed', error);
                    deferred.reject();
                });
            return deferred.promise;
        };

        this.getUserEndpoint = function(id) {
            var deferred = $q.defer();
            halAPI.from(rootUri + '/userEndpoints/' + id)
                .newRequest()
                .getResource()
                .result
                .then(
                function (response) {
                    deferred.resolve(response);
                }, function (error) {
                    MessageService.addError('Get user endpoints failed', error);
                    deferred.reject();
                });
            return deferred.promise;
        }

        return this;
    }]);
});
