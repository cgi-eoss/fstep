/**
 * @ngdoc function
 * @name fstepApp.controller:AccountCtrl
 * @description
 * # AccountCtrl
 * Controller of the account page
 */
'use strict';
define(['../../fstepmodules'], function (fstepmodules) {

    fstepmodules.controller('AccountCtrl', ['fstepProperties', '$scope', 'UserService', 'ApiKeyService', 'WalletService', 'TabService', 'MessageService', '$mdDialog', function (fstepProperties, $scope, UserService, ApiKeyService, WalletService, TabService, MessageService, $mdDialog) {


        var onUserChange = function() {
            $scope.user = UserService.params.activeUser;
            if ($scope.user.id) {
                WalletService.refreshUserTransactions('account', $scope.user);
                if ($scope.user.role !== 'ADMIN' && $scope.user.role !== 'CONTENT_AUTHORITY') {
                    $scope.checkForApiKey();
                }
            }
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

        //$scope.user = UserService.params.activeUser;






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

    }]);
});
