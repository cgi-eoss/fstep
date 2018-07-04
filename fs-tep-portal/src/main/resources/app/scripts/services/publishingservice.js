/**
 * @ngdoc service
 * @name fstepApp.PublishingService
 * @description
 * # PublishingService
 * Service in the fstepApp.
 */
'use strict';
define(['../fstepmodules', 'traversonHal'], function (fstepmodules, TraversonJsonHalAdapter) {

    fstepmodules.service('PublishingService', [ 'MessageService', 'fstepProperties', '$q', 'traverson', '$mdDialog', function (MessageService, fstepProperties, $q, traverson, $mdDialog) {

        var _this = this;

        traverson.registerMediaType(TraversonJsonHalAdapter.mediaType, TraversonJsonHalAdapter);
        var rootUri = fstepProperties.URLv2;
        var halAPI =  traverson.from(rootUri).jsonHal().useAngularHttp();
        var deleteAPI = traverson.from(rootUri).useAngularHttp();

        this.publishItem = function (item, action, type) {
            return $q(function(resolve, reject) {
                halAPI.from(rootUri + '/contentAuthority/' + type + '/' + action + '/' + item.id)
                    .newRequest()
                    .post(item)
                    .result
                    .then(
                    function (document) {
                        MessageService.addInfo(type +  ' has been published', item.name + ' has been successfully published.');
                        resolve(document);
                    }, function (error) {
                        MessageService.addError('Could not publish ' + item.name, error);
                        reject();
                    }
                );
            });
        };

        this.requestPublication = function (item, type) {
            return $q(function(resolve, reject) {
                halAPI.from(rootUri + '/publishingRequests/requestPublish' + type + '/' + item.id)
                    .newRequest()
                    .post()
                    .result
                    .then(
                    function (document) {
                        MessageService.addInfo(type +  ' publication requested', item.name + ' has been successfully requested to be made public.');
                        resolve(document);
                    }, function (error) {
                        MessageService.addError('Could not make publication request', error);
                        reject();
                    }
                );
            });
        };

        this.publishItemDialog = function ($event, item, type, onpublish) {
            function PublishItemController($scope, $mdDialog, PublishingService) {

                /* TODO: Uncomment statuses once backend implemented */
                $scope.statusResponses = {
                    GRANTED: { text: "Grant Publication", value:"publish" }/*,
                    NEEDS_INFO: { text: "Needs Info", value:"needs_info" },
                    REJECTED: { text: "Reject Publication", value:"reject" }*/
                };

                $scope.action = {};
                $scope.action.value = $scope.statusResponses.GRANTED.value;

                $scope.respond = function() {
                    PublishingService.publishItem(item, $scope.action.value, type).then(function (data) {
                        $mdDialog.hide();
                        if (onpublish) {
                            onpublish();
                        }
                    });
                };

                 $scope.closeDialog = function () {
                    $mdDialog.hide();
                };
            }
            PublishItemController.$inject = ['$scope', '$mdDialog', 'PublishingService'];
            $mdDialog.show({
                controller: PublishItemController,
                templateUrl: 'views/community/templates/publishresponse.tmpl.html',
                parent: angular.element(document.body),
                targetEvent: $event,
                clickOutsideToClose: true,
                locals: {}
            });
        };

        return this;
    }]);
});
