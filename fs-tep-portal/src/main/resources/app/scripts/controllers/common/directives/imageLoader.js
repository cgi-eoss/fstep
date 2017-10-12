define(['../../../fstepmodules'], function (fstepmodules) {
    'use strict';

    fstepmodules.directive('imageLoader', function() {
        return {
            scope: {
                source: '=',
                loading: '=',
                errorSrc: '=',
                errorAlt: '='
            },
            restrict: 'E',
            link: function(scope, element, attrs) {
                if (!scope.source) {
                    scope.status = 'error';
                }
                else {
                    scope.status = 'loading';
                    element.find('.target_image').on('load', function(event) {
                        scope.status = 'success';
                    });

                    element.find('.target_image').on('error', function(event) {
                        scope.status = 'error';
                    });
                }
            },
            templateUrl: 'views/common/directives/imageLoader.html'
        };
    });
});