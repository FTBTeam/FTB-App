import type { Meta } from '@storybook/vue3';

import { StoryObj } from '@storybook/vue3';
import UiSimpleSelector from '@/components/ui/UiSimpleSelector.vue';

const meta = {
  title: 'Ui/SimpleSelector',
  component: UiSimpleSelector,
  argTypes: {
    modelValue: {
      control: {
        type: 'text',
      },
    },
    options: {
      control: {
        type: 'object',
      },
    },
    label: {
      control: {
        type: 'text',
      },
    },
    placeholder: {
      control: {
        type: 'text',
      },
    }
  },
  tags: ['autodocs'],
  args: {
    modelValue: "",
    options: [],
    label: "Label",
    placeholder: "Placeholder"
  }
} satisfies Meta<typeof UiSimpleSelector>;

type Story = StoryObj<typeof meta>;

export const Default: Story = {
  args: {
    options: [
      { key: "test", label: "Test" }
    ],
    modelValue: "test"
  }
}

export default meta;