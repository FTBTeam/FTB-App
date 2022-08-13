<template>
  <div class="developer-options">
    <select name="" id="" @change="(value) => changeSelectedAction(value.target.value)">
      <option :value="null" selected disabled>Select an option</option>
      <option v-for="(action, index) in actions" :value="action.id" :key="index">{{ action.name }}</option>
    </select>

    <div class="options" v-if="selectedAction != null">
      <div class="option" v-for="(option, index) in selectedAction.options" :key="index">
        <label>
          {{ option.label }}
          <input
            v-if="option.type === 'number' || option.type === 'text'"
            key="option-type"
            :type="option.type"
            v-model="selectedValues[option.id]"
          />
          <select
            @change="(value) => (selectedValues[option.id] = value.target.value)"
            v-else-if="option.type === 'select'"
            key="option-type"
          >
            <option v-for="(value, key) in option.options" :value="key">{{ value }}</option>
          </select>
        </label>
      </div>
    </div>

    <button @click="runAction">Run</button>

    {{ selectedValues }}
  </div>
</template>

<script lang="ts">
import Vue from 'vue';
import Component from 'vue-class-component';
import { InstallerState } from '@/modules/app/appStore.types';
import { Action } from 'vuex-class';

type ActionContext = {
  action: Action;
  values: Record<string, string | number>;
};

type Action = {
  id: string;
  name: string;
  options: ActionOption[];
  action: (context: ActionContext) => Promise<void>;
};

type ActionOption = {
  id: string;
  label: string;
  type: 'number' | 'text' | 'select';
  options?: Record<string, string>;
  autoComplete?: (input: string) => Promise<AutoCompleteResult>;
  required?: boolean;
};

type AutoCompleteResult = {
  items: string | number;
};

@Component
export default class DeveloperPage extends Vue {
  @Action('installModpack', { namespace: 'app' }) public installModpack!: (data: InstallerState) => void;

  actions: Action[] = [
    {
      id: 'install-pack',
      name: 'Install Pack',
      options: [
        {
          id: 'pack-id',
          label: 'Pack Id',
          type: 'number',
          required: true,
        },
        {
          id: 'pack-version',
          label: 'Pack version',
          type: 'number',
          required: true,
        },
        {
          id: 'pack-type',
          label: 'Pack provider',
          type: 'select',
          options: {
            '0': 'CreeperHost',
            '1': 'CurseForge',
          },
        },
      ],
      action: async (context: ActionContext) => {
        this.installModpack({
          pack: {
            id: context.values['pack-id'],
            version: context.values['pack-version'],
            packType: context.values['pack-type'],
          },
          meta: {
            name: 'Developer installed pack',
            version: 'Your guess is as good as mine',
            art: undefined,
          },
        });
      },
    },
  ];

  selectedAction: Action | null = null;
  selectedValues: Record<string, string | number> = {};

  changeSelectedAction(id: string) {
    // Change the selection and flush the map
    this.selectedAction = this.actions.find((e) => e.id === id) ?? null;
    this.selectedValues = {};
  }

  runAction() {
    if (this.selectedAction === null) {
      return;
    }

    this.selectedAction.action({
      action: this.selectedAction,
      values: this.selectedValues,
    });
  }
}
</script>

<style lang="scss" scoped>
.developer-options {
  padding: 1.5rem;
}
</style>
