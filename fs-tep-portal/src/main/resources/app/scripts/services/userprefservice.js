/**
 * @ngdoc service
 * @name fstepApp.BasketService
 * @description
 * # BasketService
 * Service in the fstepApp.
 */
'use strict';

define(['../fstepmodules', 'traversonHal'], function(fstepmodules, TraversonJsonHalAdapter) {

    fstepmodules.service('UserPrefsService', ['$rootScope', '$http', 'fstepProperties', '$q', '$timeout', 'MessageService', 'UserService', 'traverson', function($rootScope, $http, fstepProperties, $q, $timeout, MessageService, UserService, traverson) {

        var self = this;

        traverson.registerMediaType(TraversonJsonHalAdapter.mediaType, TraversonJsonHalAdapter);
        var rootUri = fstepProperties.URLv2;
        var halAPI = traverson.from(rootUri).jsonHal().useAngularHttp();
        var deleteAPI = traverson.from(rootUri).useAngularHttp();

        
        var extractNameCB = function(type) {
            var regex = new RegExp('^' + type + '\\.');

            return function(record) {
                record.name = record.name.replace(regex, '');
            }
        }

        this.setPreference = function(type, key, value) {
            var deferred = $q.defer();
            halAPI.from(rootUri + '/userPreferences/')
                .newRequest()
                .post({
                    type: type,
                    name: type + '.' + key,
                    preference: value
                })
                .result
                .then(
                function (response) {
                    if (response.statusText == 'Created')
                        deferred.resolve(response.data);
                    else if (response.statusText == 'Conflict') 
                        deferred.reject('Conflict')
                }, function (error) {
                    MessageService.addError('Set preference failed', error);
                    deferred.reject();
                });
            return deferred.promise;
        }

        this.deletePreference = function(id) {
            var deferred = $q.defer();
            halAPI.from(rootUri + '/userPreferences/' + id)
                .newRequest()
                .delete()
                .result
                .then(
                function (response) {
                    deferred.resolve(response.data);
                }, function (error) {
                    MessageService.addError('Delete preference failed', error);
                    deferred.reject();
                });
            return deferred.promise;
        }

        
        this.updatePreference = function(id, value) {
            
            var deferred = $q.defer();
            halAPI.from(rootUri + '/userPreferences/' + id)
                .newRequest()
                .patch({
                    preference: value
                })
                .result
                .then(
                function (response) {
                    deferred.resolve(response.data);
                }, function (error) {
                    MessageService.addError('Update preference failed', error);
                    deferred.reject();
                });
            return deferred.promise;
        }

        this.updatePreferenceWithName = function(type, key, value) {
            var self = this;
            this.getPreferenceByName(type, key).then(function(record) {
                return self.updatePreference(record.id, value);
            });
        }


        this.getPreferences = function(type) {
            var deferred = $q.defer();
            halAPI.from(rootUri + '/userPreferences/search/search')
                .newRequest()
                .withRequestOptions({
                    qs: {
                        type: type,
                        owner: UserService.params.activeUser._links.self.href
                    }
                })
                .getResource()
                .result
                .then(
                function (response) {
                    var replaceCb = extractNameCB(type);
                    deferred.resolve({
                        pagingData: {
                            page: response.page,
                            _links: response._links
                        },
                        records: response._embedded.userPreferences.map(function(element){
                            replaceCb(element);
                            return element;
                        })
                    });
                }, function (error) {
                    MessageService.addError('Get preferences failed', error);
                    deferred.reject();
                });
            return deferred.promise;
        };

        this.getPreference = function(id) {
            var deferred = $q.defer();
            halAPI.from(rootUri + '/userPreferences/' + id)
                .newRequest()
                .getResource()
                .result
                .then(
                function (response) {
                    extractNameCB(response.type)(response);
                    deferred.resolve(response);
                }, function (error) {
                    MessageService.addError('Get preference failed', error);
                    deferred.reject();
                });
            return deferred.promise;
        }

        this.getPreferenceByName = function(type, name) {
            var deferred = $q.defer();
            halAPI.from(rootUri + '/userPreferences/search/search')
                .newRequest()
                .withRequestOptions({
                    qs: {
                        name: type + '.' + name,
                        owner: UserService.params.activeUser._links.self.href
                    }
                })
                .getResource()
                .result
                .then(
                function (response) {
                    var record = response._embedded.userPreferences[0];
                    extractNameCB(type)(record);
                    deferred.resolve(record);
                }, function (error) {
                    MessageService.addError('Get preferences failed', error);
                    deferred.reject();
                });
            return deferred.promise;
        }


        return this;
    }]);
});
