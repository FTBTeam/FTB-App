import type { Meta } from '@storybook/vue3';

import { StoryObj } from '@storybook/vue3';
import Message from '@/components/ui/Message.vue';
import { faCheck, faInfo, faSkullCrossbones, faWarning } from '@fortawesome/free-solid-svg-icons';

const meta = {
  title: 'Ui/Message',
  component: Message,
  render: (args) => ({
    components: { Message },
    setup: () => ({ args }),
    template: '<Message v-bind="args">{{ args.default }}</Message>',
  }),
  parameters: {
    type: {
      type: 'string',
      options: ['normal', 'success', 'info', 'warning', 'danger'],
      control: {
        type: 'select',
      },
    },
    header: {
      type: 'string',
      control: {
        type: 'text',
      },
    },
    default: {
      type: 'string',
      control: {
        type: 'text',
      },
    }
  },
  tags: ['autodocs'],
} satisfies Meta<typeof Message>;

type Story = StoryObj<typeof meta> & {
  args: {
    default: string;
  }
};

export const Default: Story = {
  args: { type: 'normal', header: 'Default Header', default: 'Default message', icon: faInfo }
}

export const DefaultNoIcon: Story = {
  args: { type: 'normal', header: 'Default Header', default: 'Default message' }
}

export const DefaultWithHeader: Story = {
  args: { type: 'normal', header: 'Default Header', default: 'Default message', icon: faInfo }
}

export const Success: Story = {
  args: { type: 'success', default: 'Success message', icon: faCheck }
}

export const SuccessWithHeader: Story = {
  args: { type: 'success', header: 'Success Header', default: 'Success message', icon: faCheck }
}

export const Info: Story = {
  args: { type: 'info', default: 'Info message', icon: faInfo }
}

export const InfoWithHeader: Story = {
  args: { type: 'info', header: 'Info Header', default: 'Info Message', icon: faInfo }
}

export const Warning: Story = {
  args: { type: 'warning', default: 'Warning message', icon: faWarning }
}

export const WarningWithHeader: Story = {
  args: { type: 'warning', header: 'Warning Header', default: 'Warning message', icon: faWarning }
}

export const Danger: Story = {
  args: { type: 'danger', default: 'Danger message', icon: faSkullCrossbones }
}

export const DangerWithHeader: Story = {
  args: { type: 'danger', header: 'Danger Header', default: 'Danger message', icon: faSkullCrossbones }
}

export default meta;