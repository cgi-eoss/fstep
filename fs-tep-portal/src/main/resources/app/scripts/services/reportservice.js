/**
 * @ngdoc service
 * @name fstepApp.ReportService
 * @description
 * # ReportService
 * Service in the fstepApp.
 */
'use strict';

define(['../fstepmodules', 'moment' ], function(fstepmodules, moment) {

    fstepmodules.service('ReportService', ['$http', 'fstepProperties', '$mdDialog', '$q', 'MessageService', function($http, fstepProperties, $mdDialog, $q, MessageService) {

        var rootUri = fstepProperties.URLv2;

        var self = this;

        this.getReportUrl = function(params) {

            let reportUrl = rootUri + '/reports/' + params.resource + (params.user ? '/' + params.user : '') + '/CSV';

            let filters = [];

            if (params.startDate) {
                filters.push('startDateTime=' + moment.utc(params.startDate).format('YYYY-MM-DD[T00:00:00Z]'));
            }
            if (params.endDate) {
                filters.push('endDateTime=' + moment.utc(params.endDate).format('YYYY-MM-DD[T23:59:59Z]'));
            }

            if (filters.length) {
                reportUrl += '?' + filters.join('&');
            }

            return reportUrl;
        };

        this.showExportDialog = function($event, resource, user) {

            function ExportController($scope, $mdDialog) {

                $scope.minDate = moment.utc().subtract(1, 'month').add(1, 'day').toDate();
                $scope.maxDate = moment.utc().toDate();

                $scope.startDate = moment.utc().subtract(1, 'month').add(1, 'day').toDate();
                $scope.endDate = moment.utc().toDate();

                if (resource === 'jobs') {
                    $scope.title = 'Export processing usage report'
                } else if (resource === 'storage') {
                    $scope.title = 'Export storage usage report'
                }

                $scope.updateExportLink = function(reason) {
                    if (reason === 'startDate' && $scope.startDate > $scope.endDate) {
                        $scope.endDate = $scope.startDate;
                    } else if (reason === 'endDate' && $scope.endDate < $scope.startDate) {
                        $scope.startDate = $scope.endDate;
                    }
                    $scope.reportLink = self.getReportUrl({
                        startDate: $scope.startDate,
                        endDate: $scope.endDate,
                        resource: resource,
                        user: user
                    });
                }

                $scope.updateExportLink();

                $scope.closeDialog = function () {
                    $mdDialog.hide();
                };

            }

            ExportController.$inject = ['$scope', '$mdDialog'];
            $mdDialog.show({
                controller: ExportController,
                templateUrl: 'views/common/templates/downloadreport.tmpl.html',
                parent: angular.element(document.body),
                targetEvent: $event,
                clickOutsideToClose: true,
                locals: {}
            });
        }


        return this;
    }]);
});
