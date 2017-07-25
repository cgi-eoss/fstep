/**
 * @ngdoc function
 * @name fstepApp.controller:AccountCtrl
 * @description
 * # AccountCtrl
 * Controller of the account page
 */
'use strict';
define(['../../fstepmodules'], function (fstepmodules) {

    fstepmodules.controller('AccountCtrl', ['fstepProperties', '$scope', 'UserService', 'WalletService', 'TabService', 'MessageService', function (fstepProperties, $scope, UserService, WalletService, TabService, MessageService) {

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
        $scope.user = undefined;

        UserService.getCurrentUser().then(function(user){
            $scope.user = user;
            WalletService.refreshUserTransactions('account', user);
        });

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
