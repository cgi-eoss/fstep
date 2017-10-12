/**
 * @ngdoc service
 * @name fstepApp.BasketService
 * @description
 * # BasketService
 * Service in the fstepApp.
 */
'use strict';

define(['../fstepmodules'], function(fstepmodules) {

    fstepmodules.service('AoiService', ['$rootScope', '$http', 'fstepProperties', '$q', '$timeout', 'UserPrefsService', function($rootScope, $http, fstepProperties, $q, $timeout, userPrefsService) {

        var self = this;

        var searchAoi = null;

        this.getSavedAois = function() {
            return userPrefsService.getPreferences('aoi');
        }

        this.getSavedAoiGeometry = function(aoi) {
            return userPrefsService.getPreference(aoi.id).then(function(data) {
                return JSON.parse(data.preference);
            });
        }

        this.saveAoi = function(name, geometry) {
            return userPrefsService.setPreference('aoi', name, JSON.stringify(geometry));
        }

        this.updateAoi = function(name, geometry) {
            return userPrefsService.updatePreferenceWithName('aoi', name, JSON.stringify(geometry));
        }

        this.deleteAoi = function(id) {
            return userPrefsService.deletePreference(id);
        }

        this.setSearchAoi = function(aoi) {
            searchAoi = aoi;
        }

        this.getSearchAoi = function() {
            return searchAoi;
        }

        return this;
    }]);
});
