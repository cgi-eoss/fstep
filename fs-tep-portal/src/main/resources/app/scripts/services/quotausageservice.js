/**
 * @ngdoc service
 * @name fstepApp.QuotaUsageService
 * @description
 * # QuotaUsageService
 * Service in the fstepApp.
 */
'use strict';

define(['../fstepmodules' ], function(fstepmodules) {

    fstepmodules.service('QuotaUsageService', ['$rootScope', '$http', 'fstepProperties', '$q', '$timeout', 'MessageService', 'UserService', function($rootScope, $http, fstepProperties, $q, $timeout, MessageService, UserService) {

        var self = this;

        var rootUri = fstepProperties.URLv2;

        this.getFileStorageUsage = function() {
            return $http.get(rootUri + '/usage/files/storage')
                .then(
                function (response) {
                    return response.data;
                }, function (error) {
                    MessageService.addError('Unable to get file storage usage ', error);
                });
        };

        this.getPersistentStorageUsage = function() {
            return $http.get(rootUri + '/usage/persistentFolder/storage')
                .then(
                function (response) {
                    return response.data;
                }, function (error) {
                    MessageService.addError('Unable to get file storage usage ', error);
                });
        };

        return this;
    }]);
});
