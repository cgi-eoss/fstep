'use strict';

module.exports = {
    dist: {
        files: [{
            expand: true,
            cwd: '<%= fstep.app %>/images',
            src: '{,*/}*.svg',
            dest: '<%= fstep.dist %>/images'
        }]
    }
};
