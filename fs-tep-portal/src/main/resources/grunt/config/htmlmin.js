'use strict';

module.exports = {
    dist: {
        options: {
            collapseWhitespace: true,
            conservativeCollapse: true,
            collapseBooleanAttributes: true,
            removeCommentsFromCDATA: true
        },
        files: [{
            expand: true,
            cwd: '<%= fstep.dist %>',
            src: ['**/*.html'],
            dest: '<%= fstep.dist %>'
        }]
    }
};
