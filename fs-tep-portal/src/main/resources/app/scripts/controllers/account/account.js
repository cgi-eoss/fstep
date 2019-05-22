/**
 * @ngdoc function
 * @name fstepApp.controller:AccountCtrl
 * @description
 * # AccountCtrl
 * Controller of the account page
 */
'use strict';
define(['../../fstepmodules'], function (fstepmodules) {

    fstepmodules.controller('AccountCtrl', ['fstepProperties', '$scope', 'UserService', 'ApiKeyService', 'WalletService', 'QuotaService', 'QuotaUsageService', 'SubscriptionService', 'ReportService', 'NavigationHelperService', 'TabService', 'MessageService', '$mdDialog', function (fstepProperties, $scope, UserService, ApiKeyService, WalletService, QuotaService, QuotaUsageService, SubscriptionService, ReportService, NavigationHelperService, TabService, MessageService, $mdDialog) {

        $scope.quotas = {};
        var subscriptionPlansMap = {};

        var setQuotaProps = function(quotaName, props) {
            if (!$scope.quotas[quotaName]) {
                $scope.quotas[quotaName] = {
                    name: quotaName,
                    subscriptionPlans: []
                };
            }
            Object.assign($scope.quotas[quotaName], props)
        }

        var refreshWallet = function() {
            WalletService.refreshWallet('account', $scope.user);
            WalletService.getTransactions('account', $scope.user._links.self.href);
        }

        var onUserChange = function() {
            $scope.user = UserService.params.activeUser;
            if ($scope.user.id) {

                refreshWallet();

                if ($scope.user.role !== 'ADMIN' && $scope.user.role !== 'CONTENT_AUTHORITY') {
                    $scope.checkForApiKey();
                }

                $scope.quotas = {};

                QuotaService.getUsageTypes().then(function(types) {

                    types.forEach(function(type) {

                        setQuotaProps(type.name, type);

                        QuotaService.getQuotaValue(type.name).then(function(value) {
                            setQuotaProps(type.name, {
                                value: value
                            });
                        })

                        QuotaUsageService.getUsageForType(type.name).then(function(usage) {
                            if (usage !== null) {
                                setQuotaProps(type.name, {
                                    usage: usage
                                });
                            }
                        })
                    })
                });


                SubscriptionService.getSubscriptionPlans().then(function(plans) {

                    var subscriptionPlans = {};

                    plans.forEach(function(plan) {
                        if (!subscriptionPlans[plan.usageType]) {
                            subscriptionPlans[plan.usageType] = [];
                        }
                        subscriptionPlans[plan.usageType].push(plan);

                        subscriptionPlansMap[plan.id] = plan;
                    })

                    for (var usageType in subscriptionPlans) {
                        setQuotaProps(usageType, {
                            subscriptionPlans: subscriptionPlans[usageType]
                        });
                    }


                    SubscriptionService.getUserSubscriptions($scope.user._links.self.href).then(function(subscriptions) {
                        subscriptions.forEach(function(subscription) {
                            if (subscription.status === 'ACTIVE') {
                                setQuotaProps(subscriptionPlansMap[subscription.subscriptionPlan.id].usageType, {
                                    subscription: subscription
                                });
                            }
                        })
                    });
                });
            }
        }

        $scope.transactionTypes = WalletService.transactionTypesMap;

        $scope.refreshTransactionFilters = function() {
            WalletService.getTransactions('account', $scope.user._links.self.href);
        }

        $scope.getPage = function(url) {
            WalletService.getTransactions('account', $scope.user._links.self.href, url);
        }

        $scope.gotoTransactionResource = function(transaction) {
            WalletService.getTransactionResource(transaction).then(function(resource) {
                if (transaction.type === 'DOWNLOAD') {
                    NavigationHelperService.goToFile(resource);
                } else if (transaction.type === 'JOB_PROCESSING') {
                    NavigationHelperService.goToJob(resource._embedded.job);
                } else if (transaction.type === 'JOB') {
                    NavigationHelperService.goToJob(resource);
                } else if (transaction.type === 'SUBSCRIPTION') {
                    showSubscriptionInfoDialog(resource);
                }
            });
        }

        /* Sidenav & Bottombar */
        $scope.navInfo = TabService.navInfo.account;
        $scope.bottombarNavInfo = TabService.navInfo.bottombar;

        /* Active session message count */
        $scope.message = {};
        $scope.message.count = MessageService.countMessages();
        $scope.$on('update.messages', function(event, job) {
            $scope.message.count = MessageService.countMessages();
        });

        $scope.toggleBottomView = function(){
            $scope.bottombarNavInfo.bottomViewVisible = !$scope.bottombarNavInfo.bottomViewVisible;
        };
        /* End Sidenav & Bottombar */

        $scope.fstepURL = fstepProperties.FSTEP_URL;
        $scope.ssoURL = fstepProperties.SSO_URL;
        $scope.walletParams = WalletService.params.account;


        $scope.checkForApiKey = function() {
            $scope.apiKeyStatus = 'loading';
            ApiKeyService.checkForApiKey().then(function(hasApiKey) {
                $scope.hasApiKey = hasApiKey;
                $scope.apiKeyStatus = 'ready';
            }).catch(function(error) {
                $scope.apiKeyStatus = 'error'
            });
        }

        $scope.generateApiKey = function() {
            if ($scope.hasApiKey) {
                var confirmDialog = $mdDialog.confirm()
                    .title('New API key generation')
                    .textContent('Generating a new key will invalidate the previous one. Would you like to continue?')
                    .ariaLabel('API key generation')
                    .ok('Yes')
                    .cancel('No');

                $mdDialog.show(confirmDialog).then(function() {

                    $scope.apiKeyStatus = 'loading';
                    ApiKeyService.regenerateApiKey().then(function(apiToken) {
                        $scope.apiKeyStatus = 'ready';
                        showApiTokenDialog(apiToken);
                    }).catch(function(error) {
                        $scope.apiKeyStatus = 'ready';
                    });
                });

            } else {
                $scope.apiKeyStatus = 'loading';
                ApiKeyService.generateApiKey().then(function(apiToken) {
                    $scope.hasApiKey = true;
                    $scope.apiKeyStatus = 'ready';
                    showApiTokenDialog(apiToken);
                }).catch(function(error) {
                    $scope.apiKeyStatus = 'ready';
                });
            }
        }

        $scope.deleteApiKey = function() {
            var confirmDialog = $mdDialog.confirm()
            .title('Confirm API key deletion')
            .textContent('Are you sure you want to delete your existing API key?')
            .ariaLabel('Confirm API key deletion')
            .ok('Yes')
            .cancel('No');

            $mdDialog.show(confirmDialog).then(function() {
                $scope.apiKeyStatus = 'loading';
                ApiKeyService.deleteApiKey().then(function() {
                    $scope.apiKeyStatus = 'ready';
                    $scope.hasApiKey = false;
                }).catch(function(error) {
                    $scope.apiKeyStatus = 'ready';
                });
            })

        }

        function showApiTokenDialog(apiToken) {

            if (apiToken) {
                $mdDialog.show({
                    controller: function($scope, $mdDialog) {

                        $scope.apiToken = apiToken;

                        $scope.hideApiTokenDialog = function() {
                            $mdDialog.hide();
                        }

                        $scope.copyTokenToClipboard = function(input) {
                            var textArea = document.createElement("textarea");
                            textArea.value = apiToken;
                            document.body.appendChild(textArea);
                            textArea.select();
                            document.execCommand('copy');
                            document.body.removeChild(textArea);
                        }
                    },
                    templateUrl: 'views/account/apitokendialog.html',
                    parent: angular.element(document.body),
                    clickOutsideToClose: false
                });
            }

        }

        $scope.showSubscriptionDialog = function(usageType) {

            var quota = $scope.quotas[usageType];

            $mdDialog.show({
                controller: function($scope, $mdDialog) {

                    $scope.quota = quota;

                    $scope.formState = {
                        selectedPlan: 'default',
                        plans: {}
                    };

                    quota.subscriptionPlans.forEach(function(plan) {
                        $scope.formState.plans[plan.id] = {
                            quantity: plan.minQuantity * plan.unit
                        }
                    });

                    if (quota.subscription) {
                        var selectedPlan;

                        if (!quota.subscription.downgradeQuantity) {
                            selectedPlan = subscriptionPlansMap[quota.subscription.subscriptionPlan.id];
                            $scope.formState.plans[selectedPlan.id].quantity =  quota.subscription.quantity * selectedPlan.unit;
                        } else {
                            selectedPlan = subscriptionPlansMap[quota.subscription.downgradePlan.id];
                            $scope.formState.plans[selectedPlan.id].quantity =  quota.subscription.downgradeQuantity * selectedPlan.unit;
                        }

                        if (quota.subscription.renew !== false ) {
                            $scope.formState.selectedPlan = selectedPlan.id;
                        }

                    }

                    var formatCoinsLabel = function(quantity) {
                        if (quantity === 1) {
                            return 'coin'
                        } else {
                            return 'coins';
                        }
                    }

                    $scope.formatSubscriptionQuantity = function() {
                        let plan = subscriptionPlansMap[quota.subscription.subscriptionPlan.id];
                        return quota.subscription.quantity * plan.unit + ' ' + quota.unit;
                    }

                    $scope.formatSubscriptionCost = function() {
                        let plan = subscriptionPlansMap[quota.subscription.subscriptionPlan.id];
                        let coins = quota.subscription.quantity * plan.costQuotation.cost;
                        return coins + ' ' + formatCoinsLabel(coins) + ' ' + $scope.formatRecurrence(plan.costQuotation.recurrence);
                    }

                    $scope.formatDowngradeQuantity = function() {
                        let plan = subscriptionPlansMap[quota.subscription.downgradePlan.id];
                        return quota.subscription.downgradeQuantity * plan.unit + ' ' + quota.unit;
                    }

                    $scope.formatDowngradeCost = function() {
                        let plan = subscriptionPlansMap[quota.subscription.downgradePlan.id];
                        let coins = quota.subscription.downgradeQuantity * plan.costQuotation.cost;
                        return coins + ' ' + formatCoinsLabel(coins) + ' ' + $scope.formatRecurrence(plan.costQuotation.recurrence);
                    }

                    $scope.formatCost = function(plan) {

                        if (plan.billingScheme === 'UNIT') {
                            return plan.costQuotation.cost + ' ' + formatCoinsLabel(plan.costQuotation.cost)
                                + ' / ' + plan.unit + ' ' + quota.unit;
                        } else {
                            return plan.costQuotation.cost  + ' ' + formatCoinsLabel(plan.costQuotation.cost)
                        }
                    }

                    $scope.computeFinalCost = function(plan) {
                        let quantity = $scope.formState.plans[plan.id].quantity;
                        if (!quantity) {
                            return 'Select the desired quantity';
                        } else {
                            let coins = quantity * plan.costQuotation.cost / plan.unit;
                            return coins + ' ' + formatCoinsLabel(coins) + ' ' + $scope.formatRecurrence(plan.costQuotation.recurrence);
                        }
                    }

                    $scope.getCurrentPlan = function() {
                        if (!quota.subscription) {
                            return 'default';
                        } else {
                            return subscriptionPlansMap[quota.subscription.subscriptionPlan.id].name
                        }
                    }

                    $scope.formatRecurrence = function(recurrence) {
                        var fRecurrence;
                        switch(recurrence) {
                            case 'HOURLY':
                                fRecurrence = 'per hour';
                                break;
                            case 'DAILY':
                                fRecurrence = 'per day';
                                break;
                            case 'MONTHLY':
                                fRecurrence = 'per month';
                                break;
                            case 'YEARLY':
                                fRecurrence = 'per year';
                                break;
                            default:
                                fRecurrence = recurrence;
                                break;
                        }

                        return fRecurrence;
                    }

                    $scope.enableSwitch = function() {
                        if (quota.subscription && quota.subscription.renew !== false) {
                            var currentPlan, currentQuantity;
                            if (!quota.subscription.downgradeQuantity) {
                                currentPlan = subscriptionPlansMap[quota.subscription.subscriptionPlan.id];
                                currentQuantity = quota.subscription.quantity;
                            } else {
                                currentPlan = subscriptionPlansMap[quota.subscription.downgradePlan.id];
                                currentQuantity = quota.subscription.downgradeQuantity;
                            }
                            if ($scope.formState.selectedPlan !== currentPlan.id) {
                                return true;
                            }
                            if ($scope.formState.plans[currentPlan.id].quantity / currentPlan.unit !== currentQuantity) {
                                return true;
                            }
                            return false;
                        } else {
                            return $scope.formState.selectedPlan !== 'default';
                        }
                    }

                    var onSubscriptionUpdated = function(subscriptionData, selectedPlan) {
                        if (subscriptionData.downgradeQuantity) {
                            Object.assign($scope.quota.subscription, subscriptionData, {
                                downgradePlan: selectedPlan
                            });
                        } else {
                            Object.assign($scope.quota.subscription, subscriptionData, {
                                subscriptionPlan: selectedPlan
                            });

                            QuotaService.getQuotaValue($scope.quota.name).then(function(value) {
                                setQuotaProps($scope.quota.name, {
                                    value: value
                                });
                            })

                            refreshWallet();
                        }

                    }

                    $scope.restoreSubscritpion = function() {

                        delete $scope.formState.errorMessage;

                        SubscriptionService.updateSubscriptionPlan($scope.quota.subscription, {
                            renew: true
                        }).then(function(data) {
                            onSubscriptionUpdated(data, subscriptionPlansMap[$scope.quota.subscription.subscriptionPlan.id]);
                        }, function(error) {
                            $scope.formState.errorMessage = 'Error updating subscription';
                        })
                    }

                    $scope.cancelSubscription = function() {

                        delete $scope.formState.errorMessage;

                        return SubscriptionService.cancelSubscriptionPlan($scope.quota.subscription).then(function() {
                            $scope.quota.subscription.renew = false;
                        }, function(error) {
                            $scope.formState.errorMessage = 'Error updating subscription';
                        });
                    }

                    $scope.cancelDowngrade = function() {
                        delete $scope.formState.errorMessage;
                        return SubscriptionService.cancelSubscriptionDowngrade($scope.quota.subscription).then(function() {
                            $scope.quota.subscription.downgradeQuantity = null;
                            $scope.quota.subscription.downgradePlan = null;
                        }, function(error) {
                            $scope.formState.errorMessage = 'Error updating subscription';
                        });
                    }

                    $scope.updateSubscription = function() {

                        delete $scope.formState.errorMessage;

                        if ($scope.formState.selectedPlan !== 'default') {

                            let selectedPlan = subscriptionPlansMap[$scope.formState.selectedPlan];

                            if ($scope.quota.subscription) {
                                SubscriptionService.updateSubscriptionPlan($scope.quota.subscription, {
                                    subscriptionPlan: selectedPlan._links.self.href,
                                    quantity: $scope.formState.plans[selectedPlan.id].quantity / selectedPlan.unit
                                }).then(function(data) {

                                    onSubscriptionUpdated(data, selectedPlan);

                                    $mdDialog.hide();
                                }, function(error) {
                                    $scope.formState.errorMessage = 'Error updating subscription';
                                });
                            } else {
                                SubscriptionService.createSubscriptionPlan({
                                    subscriptionPlan: selectedPlan._links.self.href,
                                    quantity: $scope.formState.plans[selectedPlan.id].quantity / selectedPlan.unit
                                }).then(function(data) {

                                    $scope.quota.subscription = {};
                                    onSubscriptionUpdated(data, selectedPlan);

                                    $mdDialog.hide();
                                }, function(error) {
                                    $scope.formState.errorMessage = 'Error creating subscription';
                                });
                            }

                        } else {
                            $scope.cancelSubscription().then(function() {
                                $mdDialog.hide();
                            }, function(error) {
                                $scope.formState.errorMessage = 'Error creating subscription';
                            });
                        }

                    }

                    $scope.closeSubscriptionDialog = function() {
                        $mdDialog.hide();
                    }


                },
                templateUrl: 'views/account/subscriptiondialog.html',
                parent: angular.element(document.body),
                clickOutsideToClose: false
            });
        }

        $scope.$on('active.user', onUserChange);
        onUserChange();


        $scope.hideContent = true;
        var navbar, userdetails, sidenav;
        $scope.finishLoading = function(component) {
            switch(component) {
                case 'navbar':
                    navbar = true;
                    break;
                case 'sidenav':
                    sidenav = true;
                    break;
                case 'userdetails':
                    userdetails = true;
                    break;
            }

            if (navbar && sidenav && userdetails) {
                $scope.hideContent = false;
            }
        };

        $scope.exportReport = function($event, resource) {
            ReportService.showExportDialog($event, resource);
        }

    }]);
});
