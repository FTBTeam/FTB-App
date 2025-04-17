import type { Meta } from '@storybook/vue3';

import UiSelector from './UiSelector.vue';
import { StoryObj } from '@storybook/vue3';
import UiNumberInput from '@/components/ui/UiNumberInput.vue';

const meta = {
  title: 'Ui/Input/Number Input',
  component: UiNumberInput,
  parameters: {
    modelValue: {
      type: Number,
      default: 0,
    },
    min: {
      type: Number,
      default: undefined,
    },
    max: {
      type: Number,
      default: undefined,
    },
    placeholder: {
      type: String,
      default: 'Enter a number',
    }
  },
  tags: ['autodocs'],
} satisfies Meta<typeof UiSelector>;

type Story = StoryObj<typeof meta>;

export const Default: Story = {
  args: {}
}

export default meta;