/**
 * @ngdoc function
 * @name fstepApp.controller:AdminCtrl
 * @description
 * # AdminCtrl
 * Controller of the admin page
 */
'use strict';
define(['../../fstepmodules'], function (fstepmodules) {

    fstepmodules.controller('AdminCtrl', ['$scope', 'UserService', 'MessageService', 'WalletService', 'QuotaService', 'ReportService', 'TabService', function ($scope, UserService, MessageService, WalletService, QuotaService, ReportService, TabService) {

        /* Sidenav & Bottombar */
        $scope.navInfo = TabService.navInfo.admin;
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

        $scope.userParams = UserService.params.admin;
        $scope.roles = ['USER', 'EXPERT_USER', 'CONTENT_AUTHORITY', 'ADMIN'];

        /* Paging */
        $scope.getPage = function(url){
            UserService.getUsersPage('admin', url);
        };

        UserService.getUsersByFilter('admin');

        QuotaService.getUsageTypes().then(function(types) {
            $scope.quotaUsageTypes = types.map(function(type) {
                return type.name;
            });
        })

        $scope.filter = function(){
            UserService.getUsersByFilter('admin');
        };

        $scope.getUserData = function(){
            if($scope.userParams.selectedUser){
                UserService.getUserByLink($scope.userParams.selectedUser._links.self.href).then(function(data){
                    $scope.userParams.userDetails = data;
                });

                WalletService.getUserWallet($scope.userParams.selectedUser).then(function(wallet){
                   $scope.userParams.wallet = wallet;
                });


                QuotaService.getUserQuotas($scope.userParams.selectedUser._links.self.href).then(function(quotas) {
                    $scope.userParams.quotas = {};
                    quotas.forEach(function(quota) {
                        $scope.userParams.quotas[quota.usageType.name] = quota;
                    })
                });


            }
        };

        $scope.addCoins = function() {
            WalletService.makeTransaction($scope.userParams.selectedUser, $scope.userParams.wallet, $scope.userParams.coins).then(function(){
                $scope.userParams.coins = 0;
                $scope.getUserData();
            });
        };

        $scope.updateRole = function(newRole) {
            $scope.userParams.userDetails.role = newRole;
            UserService.updateUser($scope.userParams.userDetails).then(function(data){
                $scope.getUserData();
            });
        };

        $scope.updateQuotas = function() {
            for (var usageType in $scope.userParams.quotas) {
                let quota = $scope.userParams.quotas[usageType];
                quota.usageType = usageType;
                QuotaService.setQuotaForUser($scope.userParams.selectedUser._links.self.href, quota).then(function(response) {
                    $scope.userParams.quotas[usageType] = response;
                })
            }
        }

        $scope.exportReport = function($event, resource) {
            ReportService.showExportDialog($event, resource, $scope.userParams.selectedUser.id)
        }

        $scope.hideContent = true;
        var navbar, sidenav, management;
        $scope.finishLoading = function(component) {
            switch(component) {
                case 'navbar':
                    navbar = true;
                    break;
                case 'sidenav':
                    sidenav = true;
                    break;
                case 'management':
                    management = true;
                    break;
            }

            if (navbar && sidenav && management) {
                $scope.hideContent = false;
            }
        };

    }]);
});
