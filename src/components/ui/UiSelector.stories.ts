import type { Meta } from '@storybook/vue3';

import UiSelector from './UiSelector.vue';
import { StoryObj } from '@storybook/vue3';

const meta = {
  title: 'Ui/Selector',
  component: UiSelector,
  render: () => ({
    components: { UiSelector },
    template: '<UiSelector />',
  }),
  parameters: {
    
  },
  tags: ['autodocs'],
} satisfies Meta<typeof UiSelector>;

type Story = StoryObj<typeof meta>;

export const Default: Story = {
  args: {}
}

export default meta;