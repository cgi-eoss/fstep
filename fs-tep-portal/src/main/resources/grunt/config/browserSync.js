'use strict';

module.exports = {
    default_options: {
        bsFiles: {
            src: [
                'app/**/*.css',
                'app/styles/*.css',
                'app/scripts/**/*.js',
                '<%= fstep.app %>/index.html',
                "<%= fstep.app %>/views/**/*.html"
            ]
        },
        options: {
            port: 9000,
            ui: {
                port: 9001
            },
            watchTask: true,
            injectChanges: true,
            server: {
                baseDir: 'app',
                routes: {
                    "/bower_components": "bower_components"
                }
            }
        }
    }
};
