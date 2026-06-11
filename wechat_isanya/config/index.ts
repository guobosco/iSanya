import { defineConfig } from '@tarojs/cli';
import devConfig from './dev';
import prodConfig from './prod';

export default defineConfig(async (merge, { command, mode }) => {
  const baseConfig = {
    projectName: 'wechat_isanya',
    date: '2026-06-11',
    designWidth: 750,
    sourceRoot: 'src',
    outputRoot: 'dist',
    plugins: [],
    alias: {
      '@': '/src',
    },
    framework: 'react',
    compiler: {
      type: 'vite',
    },
    mini: {
      postcss: {
        pxtransform: {
          enable: true,
          config: {},
        },
        cssModules: {
          enable: true,
          config: {
            namingPattern: 'module',
            generateScopedName: '[name]__[local]___[hash:base64:5]',
          },
        },
      },
    },
    h5: {
      publicPath: '/',
      staticDirectory: 'static',
    },
  };

  if (command === 'build' && mode === 'production') {
    return merge({}, baseConfig, prodConfig);
  }

  return merge({}, baseConfig, devConfig);
});
