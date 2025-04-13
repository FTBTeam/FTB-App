import type { Preview } from '@storybook/vue3'
import "../src/styles.css"
import '@/assets/fonts.scss';
import '@/assets/global.scss';
import 'wysiwyg.css/wysiwyg.css';
import 'balloon-css/balloon.css';
import '@/assets/liboverrides.scss';

const preview: Preview = {
  parameters: {
    backgrounds: {
      values: [
        { name: 'Dark', value: '#2A2A2A' },
      ],
      default: 'dark',
    },
    controls: {
      matchers: {
       date: /Date$/i,
      },
    },
  },
};

export default preview;