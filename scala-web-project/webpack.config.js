var webpack = require('webpack');
module.exports = {
  entry: './ui/entry.js',
  output: { path: __dirname + '/public/compiled', filename: 'bundle.js' },
  module: {
    /*
      Out of the box, Webpack can work only with standard JavaScript files.
      It uses loaders to work with everything else.
      Here we’re specifying the regex that limits the babel-loader to process only js and jsx files.
      The query part instructs Babel to use ES2015, React and Stage-0 presets.
      The ES2015 preset allows us to use common ES6 features such as let and const, whereas Stage-0 adds support for arrow functions.
    */
    loaders: [
      { test: /\.jsx?$/, loader: 'babel-loader',
        include: /ui/, query: { presets: ['es2015', 'stage-0', 'react'] } }
    ]
} }
