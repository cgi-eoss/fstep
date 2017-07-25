'use strict';

module.exports = {
    dist: {
        files: [{
            expand: true,
            cwd: '<%= fstep.app %>/images',
            src: '{,*/}*.{png,jpg,jpeg,gif}',
            dest: '<%= fstep.dist %>/images'
        }]
    }
};
