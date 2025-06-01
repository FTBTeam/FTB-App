import type { Meta } from '@storybook/vue3';

import { StoryObj } from '@storybook/vue3';
import InputNumber from '@/components/ui/form/InputNumber/InputNumber.vue';
import TextArea from '@/components/ui/form/TextArea/TextArea.vue';

const meta = {
  title: 'Ui/Form/TextArea',
  component: TextArea,
  parameters: {
    modelValue: {
      type: String,
      default: "",
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