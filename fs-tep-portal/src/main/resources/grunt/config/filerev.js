'use strict';

// Renames files for browser caching purposes
module.exports = {
        dist: {
            src: [
              '<%= fstep.dist %>/scripts/{,*/}*.js',
              '<%= fstep.dist %>/styles/{,*/}*.css',
              '<%= fstep.dist %>/images/{,*/}*.{png,jpg,jpeg,gif,webp,svg}',
              '<%= fstep.dist %>/styles/fonts/*'
            ]
        }
};
