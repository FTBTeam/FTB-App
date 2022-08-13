<template>
  <div class="developer-options">
    <div class="actions">
      <h2>Actions</h2>

      <div class="search">
        <input type="search" placeholder="Search" v-model="search" />
      </div>

      <div class="empty" v-if="search !== '' && filteredActions.length === 0">No results found for {{ search }}</div>

      <div
        class="action"
        v-for="(action, index) in filteredActions"
        :key="index"
        @click="changeSelectedAction(action.id)"
      >
        <div class="title">{{ action.name }}</div>
        <div class="desc">{{ action.description }}</div>
        <div class="meta">
          <div class="item">Options: {{ action.options.length }}</div>
          <div class="item">Id: {{ action.id }}</div>
        </div>
      </div>
    </div>

    <div class="options-section" v-if="selectedAction">
      <h2>Action options</h2>
      <div class="options">
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
    </div>
  </div>
</template>

<script lang="ts">
import Vue from 'vue';
import Component from 'vue-class-component';
import { InstallerState } from '@/modules/app/appStore.types';
import { Action } from 'vuex-class';

export type ActionContext = {
  action: Action;
  values: Record<string, string | number>;
};

export type Action = {
  id: string;
  name: string;
  description: string;
  options: ActionOption[];
  action: (context: ActionContext) => Promise<void>;
};

export type ActionOption = {
  id: string;
  label: string;
  description?: string;
  type: 'number' | 'text' | 'select';
  options?: Record<string, string>;
  autoComplete?: (input: string) => Promise<AutoCompleteResult>;
  required?: boolean;
};

export type AutoCompleteResult = {
  items: string | number;
};

@Component
export default class DeveloperPage extends Vue {
  @Action('installModpack', { namespace: 'app' }) public installModpack!: (data: InstallerState) => void;

  actions: Action[] = [
    {
      id: 'install-pack',
      name: 'Install Pack',
      description: 'Allows you to install a modpack from a given pack id and version id',
      options: [
        {
          id: 'pack-id',
          label: 'Pack Id',
          description: 'The packs id from modpacks.ch or curseforge',
          type: 'number',
          required: true,
        },
        {
          id: 'pack-version',
          label: 'Pack version',
          description: 'The packs version from modpacks.ch or curseforge',
          type: 'number',
          required: true,
        },
        {
          id: 'pack-type',
          label: 'Pack provider',
          description: 'Pull from a selected modpack provider',
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

  search = '';

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

  get filteredActions() {
    return this.actions.filter((e) => e.name.toLowerCase().includes(this.search));
  }
}
</script>

<style lang="scss" scoped>
.developer-options {
  padding: 1.5rem;
  display: grid;
  grid-template-columns: 0.3fr 0.7fr;

  font-size: 14px;
  height: 100%;
  background-color: #202429;

  h2 {
    font-size: 1.25em;
    font-weight: bold;
    margin-bottom: 1em;
  }

  input,
  select {
    border-radius: 5px;
    background-color: #171c1f;
    border: 1px solid #333940;
    width: 100%;
    line-height: 1.8em;
    padding: 0.4em 1em;
    color: white;

    &::placeholder {
      color: rgba(white, 0.6);
    }
  }

  select {
    padding: 0.6em 1em;
  }

  .actions {
    overflow-y: auto;

    .search {
      margin-bottom: 1em;
    }

    .action {
      padding: 1em;
      background-color: #293037;
      border-radius: 5px;

      .title {
        color: white;
        font-weight: bold;
        margin-bottom: 0.4em;
      }

      .desc {
        opacity: 0.8;
      }

      .meta {
        margin-top: 0.7em;
        display: flex;
        gap: 0.5em;

        .item {
          padding: 0.2em 0.8em;
          background-color: black;
          border-radius: 3px;
          color: rgba(white, 0.6);
          font-size: 0.875em;
        }
      }
    }
  }

  .options-section {
    padding-left: 3em;

    .option {
      margin-bottom: 0.8em;

      input,
      select {
        margin-top: 0.5em;
      }
    }

    button {
      display: inline-block;
      background-color: #4dbb2c;
      padding: 0.5em 3em;
      border-radius: 5px;
      margin-top: 1em;
      color: white;
    }
  }
}
</style>
