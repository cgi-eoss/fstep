'use strict';

module.exports = {
    dist: {
        options: {
            module: 'fstepApp',
            htmlmin: '<%= htmlmin.dist.options %>'
        },
        cwd: '<%= fstep.app %>',
        src: 'views/{,*/}*.html',
        dest: '<%= fstep.dist %>/scripts/templateCache.js'
    }
};
