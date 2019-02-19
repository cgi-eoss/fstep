/**
 * @ngdoc service
 * @name fstepApp.QuotaService
 * @description
 * # QuotaService
 * Service in the fstepApp.
 */
'use strict';

define(['../fstepmodules' ], function(fstepmodules) {

    fstepmodules.service('QuotaService', ['$rootScope', '$http', 'fstepProperties', '$q', '$timeout', 'MessageService', 'UserService', function($rootScope, $http, fstepProperties, $q, $timeout, MessageService, UserService) {

        var self = this;

        var rootUri = fstepProperties.URLv2;

        this.getUsageTypes = function() {
            return $http.get(rootUri + '/quotas/usageTypes')
                .then(
                function (response) {
                    return response.data;
                }, function (error) {
                    MessageService.addError('Unable to get quota usage types', error);
                });
        };

        this.getQuotaValue = function(usageType) {
            return $http.get(rootUri + '/quotas/value?usageType=' + usageType).then(function(response) {
                return response.data;
            }, function(error) {
                MessageService.addError('Unable to get user quota for ' + usageType, error);
            })
        }

        this.getUserQuotas = function(user) {
            return $http.get(rootUri + '/quotas/search/findByOwner?owner=' + user).then(function(response) {
                return response.data._embedded.quotas;
            }, function(error) {
                MessageService.addError('Unable to get user quota', error);
            })
        }

        this.setQuotaForUser = function(user, quota) {
            if (quota.id) {
                if (quota.value) {
                    return $http.patch(rootUri + '/quotas/' + quota.id, {value: quota.value}).then(function(response) {
                        return response.data;
                    }, function(error) {
                        MessageService.addError('Unable to update user quota', error);
                    })
                } else {
                    return $http.delete(rootUri + '/quotas/' + quota.id).then(function(response) {
                        return response;
                    }, function(error) {
                        MessageService.addError('Unable to delete user quota', error);
                    })
                }
            } else {
                if (quota.value) {
                    let postData = Object.assign({}, quota, {owner: user});
                    return $http.post(rootUri + '/quotas/', postData, {headers: {'Content-Type': 'application/json'}}).then(function(response) {
                        return response.data;
                    }, function(error) {
                        MessageService.addError('Unable to set user quota', error);
                    })
                } else {
                    return $q.resolve(quota);
                }
            }
        }

        return this;
    }]);
});
