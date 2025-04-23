import type { Meta } from '@storybook/vue3';

import { StoryObj } from '@storybook/vue3';
import Input from '@/components/ui/form/Input/Input.vue';
import { faSave } from '@fortawesome/free-solid-svg-icons';

const meta = {
  title: 'Ui/Form/Input',
  component: Input,
  parameters: {
    modelValue: {
      type: Number,
      default: 0,
    },
    placeholder: {
      type: String,
      default: 'Your name',
    }
  },
  tags: ['autodocs'],
} satisfies Meta<typeof Input>;

type Story = StoryObj<typeof meta>;

export const Default: Story = {
  args: {}
}

export const Iconed: Story = {
  args: {
    icon: faSave
  }
}

export const Labeled: Story = {
  args: {
    label: 'Name'
  }
}

export const Placeholdered: Story = {
  args: {
    label: 'Name',
    placeholder: 'Your name'
  }
}

export const FullWidth: Story = {
  args: {
    label: 'Name',
    placeholder: 'Your name',
    fill: true
  }
}

export default meta;