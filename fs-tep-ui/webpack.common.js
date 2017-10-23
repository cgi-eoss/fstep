const path = require('path');

const webpack = require('webpack');
const ExtractTextPlugin = require('extract-text-webpack-plugin');
const HtmlWebpackPlugin = require('html-webpack-plugin');

const cssPipeline = [
    {
        loader: 'css-loader',
        options: {
            importLoaders: 1,
            sourceMap: true,
            minimize: true
        }
    },
    {
        loader: 'postcss-loader',
        options: {
            plugins: () => [
                require('postcss-import')({}),
                require('postcss-url')({}),
                require('postcss-cssnext')({
                    browsers: ['last 2 versions', 'ie >= 11'],
                    compress: true,
                })
            ]
        }
    }
]

const sassPipeline = [
    ...cssPipeline,   
    {
       loader: 'sass-loader'
    }
];

//sassPipeline[0].options.importLoaders = 2;


const config = ((env = {}) => {
    return {
        entry: {
            app: './src/main.ts'
        },
        output: {
            path: env.outpath || path.resolve(__dirname, "dist"),
            filename: '[name].[hash].bundle.js',
        },
        resolve: {
            modules: [path.resolve(__dirname, "src"), "node_modules"],
            extensions: ['.ts', '.js']
        },
        module: {
            rules: [
                {
                    test: /\.css$/,
                    exclude: /\.component\.css$/,
                    use: ExtractTextPlugin.extract({
                        use: cssPipeline
                    })
                },

                {
                    test: /\.(sass|scss)$/,
                    exclude: /\.component\.(sass|scss)$/,
                    use: ExtractTextPlugin.extract({
                        use: sassPipeline
                    })
                },
                                {
                    test: /\.component\.css$/,
                    use: [
                        {
                            loader: 'to-string-loader',
                        },
                        ...cssPipeline
                    ]
                },
                {
                    test: /\.component\.(sass|scss)$/,
                    use: [
                        {
                            loader: 'to-string-loader',
                        },
                        ...sassPipeline
                    ]
                },
                {
                    test: /\.(png|jpe?g|gif)$/i,
                    use: [
                        {
                            loader: 'file-loader',
                            options: {
                                name: 'assets/images/[name].[hash].[ext]'
                            }
                        }
                    ]
                },
                {
                    test: /\.woff($|\?)|\.woff2($|\?)|\.ttf($|\?)|\.eot($|\?)|\.svg($|\?)/,
                    use: [
                        {
                            loader: 'file-loader',
                            options: {
                                name: 'assets/fonts/[name].[hash].[ext]'
                            }
                        }
                    ]
                },
                {
                    test: /\.html$/,
                    use: [{
                        loader: 'html-loader',
                        options: {
                            minimize: false
                        }
                    }]
                }
            ]
        },
        plugins: [
            new webpack.ContextReplacementPlugin(
                // The (\\|\/) piece accounts for path separators in *nix and Windows
                /angular(\\|\/)core(\\|\/)@angular/,
                path.resolve(__dirname, 'src'), // location of your src
                {} // a map of your routes
            ),
            new ExtractTextPlugin({
                filename: '[name].bundle.css'
            }),
            new HtmlWebpackPlugin({
                //inject: 'head',
                //hash: true,
                template: './src/index.ejs',
                baseUrl: env.baseUrl || '/analyst/'
            })
        ]
    }
});


module.exports = config;
