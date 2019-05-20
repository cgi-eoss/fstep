define(['../fstepmodules', 'traversonHal' ], function(fstepmodules, TraversonJsonHalAdapter) {

    fstepmodules.service('CostingExpressionService', ['fstepProperties', '$q', 'traverson', '$mdDialog', 'MessageService', 'CommonService', function(fstepProperties, $q, traverson, $mdDialog, MessageService, CommonService) {

        traverson.registerMediaType(TraversonJsonHalAdapter.mediaType, TraversonJsonHalAdapter);
        var halAPI =  traverson.from(rootUri).jsonHal().useAngularHttp();
        var rootUri = fstepProperties.URLv2;


        var self = this;

        this.showItemCostDialog = function($event, resourceType, resourceId) {

            this.getCostingExpression(resourceType, resourceId).then(function(costingExpression) {

                function SetItemCostController($scope, $mdDialog) {

                    $scope.costingExpression = {
                        costExpression: parseFloat(costingExpression.costExpression),
                    }

                    $scope.itemName = resourceType;

                    $scope.updateCostingExpression = function() {
                        self.setCostingExpression(resourceType, resourceId, $scope.costingExpression).then(function() {
                            $mdDialog.hide();
                        });
                    }

                    $scope.closeDialog = function () {
                        $mdDialog.hide();
                    };
                }

                SetItemCostController.$inject = ['$scope', '$mdDialog'];
                $mdDialog.show({
                    controller: SetItemCostController,
                    templateUrl: 'views/common/templates/setitemcost.tmpl.html',
                    parent: angular.element(document.body),
                    targetEvent: $event,
                    clickOutsideToClose: true,
                    locals: {}
                });
            });

        }

        this.getCostingExpression = function(resourceType, resourceId) {
            return $q(function(resolve, reject) {
                halAPI.from(rootUri + '/cost/' + resourceType + '/' + resourceId)
                .newRequest()
                .getResource()
                .result
                .then(function (document) {
                    resolve(document);
                 }, function (error) {
                    MessageService.addError('Unable to get costing expression for ' + resourceType, error);
                    reject(error);
                 });
            });
        }

        this.setCostingExpression = function(resourceType, resourceId, costingExpression) {
            return $q(function(resolve, reject) {
                halAPI.from(rootUri + '/contentAuthority/' + resourceType + 's/costingExpression/' + resourceId)
                    .newRequest()
                    .post(costingExpression)
                    .result
                    .then(
                    function (document) {
                        MessageService.addInfo('Costing expression updated', 'Costing expression for ' + resourceType + ' has been updated');
                        resolve(document);
                    }, function (error) {
                        MessageService.addError('Could not update costing expression for ' + resourceType, error);
                        reject();
                    }
                );
            });
        }

        return this;

    }]);
});
