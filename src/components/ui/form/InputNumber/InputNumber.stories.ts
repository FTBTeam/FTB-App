import type { Meta } from '@storybook/vue3';

import { StoryObj } from '@storybook/vue3';
import InputNumber from '@/components/ui/form/InputNumber/InputNumber.vue';

const meta = {
  title: 'Ui/Form/Number Input',
  component: InputNumber,
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
} satisfies Meta<typeof InputNumber>;

type Story = StoryObj<typeof meta>;

export const Default: Story = {
  args: {}
}

export default meta;