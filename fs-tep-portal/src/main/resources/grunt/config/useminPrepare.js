'use strict';

/* Reads HTML for usemin blocks to enable smart builds that automatically
 * concat, minify and revision files. Creates configurations in memory so
 * additional tasks can operate on them. */
module.exports = {
    html: '<%= fstep.app %>/index.html',
    css: ['<%= fstep.app %>/main.css'],
    options: {
        dest: '<%= fstep.dist %>',
        flow: {
            html: {
                steps: {
                    js: ['concat', 'uglifyjs'],
                    css: ['cssmin']
                },
                post: {}
            }
        }
    }
};
