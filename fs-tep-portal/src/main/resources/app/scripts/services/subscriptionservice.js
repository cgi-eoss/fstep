/**
 * @ngdoc service
 * @name fstepApp.QuotaUsageService
 * @description
 * # QuotaUsageService
 * Service in the fstepApp.
 */
'use strict';

define(['../fstepmodules' ], function(fstepmodules) {

    fstepmodules.service('SubscriptionService', ['$rootScope', '$http', 'fstepProperties', '$q', '$timeout', 'MessageService', 'UserService', function($rootScope, $http, fstepProperties, $q, $timeout, MessageService, UserService) {

        var self = this;

        var rootUri = fstepProperties.URLv2;

        this.getSubscriptionPlans = function() {
            return $http.get(rootUri + '/subscriptionPlans')
                .then(
                function (response) {
                    let requests = response.data._embedded.subscriptionPlans.map(function(plan) {
                        return $http.get(rootUri + '/subscriptionPlans/' + plan.id).then(function(response) {
                            return response.data;
                        });
                    });

                    return $q.all(requests);
                }, function (error) {
                    MessageService.addError('Unable to get subscription plans: ', error);
                });
        };

        this.getUserSubscriptions = function(user) {
            return $http.get(rootUri + '/subscriptions/search/findByOwner?owner=' + user + '&projection=detailedSubscription')
                .then(
                function (response) {
                    return response.data._embedded.subscriptions;
                }, function (error) {
                    MessageService.addError('Unable to get subscriptions ', error);
                });
        };

        this.createSubscriptionPlan = function(data) {
            return $http.post(rootUri + '/subscriptions', data, {headers: {'Content-Type': 'application/json'}}).then(
                function(response) {
                    return response.data;
                }, function(error) {
                    MessageService.addError('Unable to create subscription: ', error);
                    throw error;
                }
            )
        }

        this.updateSubscriptionPlan = function(subscription, data) {
            return $http.patch(rootUri + '/subscriptions/' + subscription.id, data, {headers: {'Content-Type': 'application/json'}}).then(
                function(response) {
                    return response.data;
                }, function(error) {
                    MessageService.addError('Unable to update subscription: ', error);
                    throw error;
                }
            )
        }

        this.cancelSubscriptionPlan = function(subscription) {
            return $http.post(rootUri + '/subscriptions/' + subscription.id + '/cancel').then(function(response) {
                return response
            }, function(error) {
                MessageService.addError('Unable to cancel subscription: ', error);
                throw error;
            });
        }

        this.cancelSubscriptionDowngrade = function(subscription) {
            return $http.patch(rootUri + '/subscriptions/' + subscription.id, {
                downgradeQuantity: null,
                downgradePlan: null
            }, {headers: {'Content-Type': 'application/json'}}).then(function(response) {
                return response
            }, function(error) {
                MessageService.addError('Unable to cancel subscription downgrade: ', error);
                throw error;
            });
        }

        this.deleteSubscriptionPlan = function(subscription) {
            return $http.delete(rootUri + '/subscriptions/' + subscription.id).then(function(response) {
                return response
            }, function(error) {
                MessageService.addError('Unable to delete subscription: ', error);
                throw error;
            });
        }

        return this;
    }]);
});
