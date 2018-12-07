define(['../../../fstepmodules'], function (fstepmodules) {
    'use strict';

    fstepmodules.directive('dateRangeField', function() {
        return {
            scope: {
                label: '=',
                description: '=',
                value: '=',
                onChange: '&'
            },
            restrict: 'E',
            link: function(scope, element, attrs) {
                scope.onStartDateChange = function() {
                    if (!scope.value.end || scope.value.end < scope.value.start) {
                        scope.value.end = scope.value.start;
                    }
                    if (scope.onChange) {
                        scope.onChange(scope.value);
                    }
                }

                scope.onEndDateChange = function() {
                    if (!scope.value.start || scope.value.start > scope.value.end) {
                        scope.value.start = scope.value.end;
                    }
                    if (scope.onChange) {
                        scope.onChange(scope.value);
                    }
                }
            },
            templateUrl: 'views/common/directives/dateRangeField.html'
        };
    });
});
