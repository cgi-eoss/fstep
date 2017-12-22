/**
 * @ngdoc service
 * @name fstepApp.BasketService
 * @description
 * # BasketService
 * Service in the fstepApp.
 */
'use strict';

define(['../fstepmodules', 'traversonHal'], function(fstepmodules, TraversonJsonHalAdapter) {

    fstepmodules.service('UserMountsService', ['$rootScope', '$http', 'fstepProperties', '$q', '$timeout', 'MessageService', 'UserService', 'traverson', function($rootScope, $http, fstepProperties, $q, $timeout, MessageService, UserService, traverson) {

        var self = this;

        traverson.registerMediaType(TraversonJsonHalAdapter.mediaType, TraversonJsonHalAdapter);
        var rootUri = fstepProperties.URLv2;
        var halAPI = traverson.from(rootUri).jsonHal().useAngularHttp();
        var deleteAPI = traverson.from(rootUri).useAngularHttp();

        this.getUserMounts = function(type) {
            var deferred = $q.defer();
            halAPI.from(rootUri + '/userMounts/')
                .newRequest()
                .getResource()
                .result
                .then(
                function (response) {
                    deferred.resolve(response._embedded.userMounts);
                }, function (error) {
                    MessageService.addError('Get user mounts failed', error);
                    deferred.reject();
                });
            return deferred.promise;
        };

        /*
        this.getUserMounts = function(type) {
            var deferred = $q.defer();
            setTimeout(function() {
                var mounts = [{id: 44, name: 'vistamount1'}, {id: 45, name: 'vistamount2'}, {id: 46, name: 'vistamount3'}];
                deferred.resolve(mounts);
            }, 200);

            return deferred.promise;
        }
        */

        return this;
    }]);
});
