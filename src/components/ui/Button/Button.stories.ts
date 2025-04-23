import type { Meta } from '@storybook/vue3';

import { StoryObj } from '@storybook/vue3';
import Button from './Button.vue';

const meta = {
  title: 'Ui/Button',
  component: Button,
  parameters: {
  },
  tags: ['autodocs'],
} satisfies Meta<typeof Button>;

type Story = StoryObj<typeof meta>;

export const Default: Story = {
  args: {
  }
}

export default meta;