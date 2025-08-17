<script lang="ts" setup>
  import UiButton from '@/components/ui/UiButton.vue';
  import {onMounted, ref} from "vue";
  import {Input, Loader, Message, ProgressBar, UiBadge, UiToggle} from "@/components/ui";
  import {faSearch} from "@fortawesome/free-solid-svg-icons";
  import UiSelect from "@/components/ui/select/UiSelect.vue";
  import Modal from "@/components/ui/modal/Modal.vue";
  import {SelectOption} from "@/components/ui/select/UiSelectType";
  
  const testInput = ref("Test input")
  const testToggle = ref(false);
  
  const selectModalOpen = ref(false);
  const testProgress = ref(0);
  
  const options: SelectOption[] = [
    {key: 'option1', label: 'Option 1'},
    {key: 'option2', label: 'Option 2'},
    {key: 'option3', label: 'Option 3'},
    {key: 'option4', label: 'Option 4'},
  ]
  
  onMounted(() => {
    setInterval(() => {
      testProgress.value += 0.1;
      if (testProgress.value > 1) {
        testProgress.value = 0;
      }
    }, 1000);
  })
</script>

<template>
  <p class="font-bold uppercase mb-4 text-white/80">Buttons</p>
  <div class="flex flex-wrap gap-2 mb-6">
    <UiButton>Default</UiButton>
    <UiButton type="primary">Primary</UiButton>
    <UiButton type="success">Success</UiButton>
    <UiButton type="info">Info</UiButton>
    <UiButton type="warning">Warning</UiButton>
    <UiButton type="danger">Danger</UiButton>
  </div>

  <p class="font-bold uppercase mb-4 text-white/80">Input</p>
  
  <div class="flex gap-4 flex-col mb-8">
    <div class="flex flex-col gap-2">
      <p>No Label</p>
      <Input v-model="testInput" />
    </div>
    <div class="flex flex-col gap-2">
      <p>With Label</p>
      <Input label="Hello, I'm a label" v-model="testInput" />
    </div>
    <div class="flex flex-col gap-2">
      <p>Full Width</p>
      <Input fill label="I'm a full boi" v-model="testInput" />
    </div>
    <div class="flex flex-col gap-2">
      <p>Disabled</p>
      <Input disabled label="Disabled input" v-model="testInput" />
    </div>
    <div class="flex flex-col gap-2">
      <p>With Icon</p>
      <Input :icon="faSearch" label="Disabled input" v-model="testInput" />
    </div>
  </div>

  <p class="font-bold uppercase mb-4 text-white/80">Toggle</p>
  <div class="flex gap-4 flex-col mb-8">
    <div class="flex flex-col gap-2">
      <p>Standard</p>
      <UiToggle v-model="testToggle" />
    </div>
    <div class="flex flex-col gap-2">
      <p>Label</p>
      <UiToggle label="Hello, I'm a label" v-model="testToggle" />
    </div>
    <div class="flex flex-col gap-2">
      <p>Disabled</p>
      <UiToggle label="Disabled toggle" disabled v-model="testToggle" />
    </div>
    <div class="flex flex-col gap-2">
      <p>Right aligned</p>
      <UiToggle alignRight label="Look mum! I'm on the right" v-model="testToggle" />
    </div>
    <div class="flex flex-col gap-2">
      <p>Label & Desc</p>
      <UiToggle label="Hello, I'm a label" desc="Look, this is a description, you can write text here, it's pretty cool actually..." v-model="testToggle" />
    </div>
  </div>
  
  <p class="font-bold uppercase mb-4 text-white/80">Select</p>
  <div class="mb-8">
    <UiButton @click="selectModalOpen = true">Show modal with select</UiButton>
    <Modal :open="selectModalOpen" @closed="selectModalOpen = false" title="Select Modal" subTitle="This is a modal with a select component">
      <div class="pb-[400px]"></div>
      <UiSelect :options="options" />
      <div class="pb-[400px]"></div>
      <template #footer>
        <div class="flex gap-4 justify-end">
          <UiButton @click="selectModalOpen = false">Close</UiButton>
        </div>
      </template>
    </Modal>
  </div>
  
  <div class="mb-8">
    <UiSelect :options="options">      
      <template #option="{ option }">
        <div class="p-2 hover:bg-white/10 transition-colors duration-200">
          {{ option.label }}
        </div>
      </template>
    </UiSelect>
  </div>

  <p class="font-bold uppercase mb-4 text-white/80">Badges</p>
  
  <div class="flex gap-4 flex-wrap mb-8">
    <UiBadge type="primary">Primary</UiBadge>
    <UiBadge type="success">Success</UiBadge>
    <UiBadge type="info">Info</UiBadge>
    <UiBadge type="warning">Warning</UiBadge>
    <UiBadge type="danger">Danger</UiBadge>
    <UiBadge>Default</UiBadge>
  </div>

  <p class="font-bold uppercase mb-4 text-white/80">Progress</p>

  <div class="flex gap-4 flex-col mb-8">
    <div class="flex flex-col gap-2">
      <p>Default</p>
      <ProgressBar :progress="testProgress" />
    </div>
    <div class="flex flex-col gap-2">
      <p>Infinite</p>
      <ProgressBar infinite />
    </div>
    <div class="flex flex-col gap-2">
      <p>With progress</p>
      <ProgressBar :progress=".5" />
    </div>
    <div class="flex flex-col gap-2">
      <p>No working state</p>
      <ProgressBar :progress=".5" :do-progress-animation="false" />
    </div>
    <div class="flex flex-col gap-2">
      <p>Muted</p>
      <ProgressBar type="muted" :progress=".5" />
    </div>
    <div class="flex flex-col gap-2">
      <p>Inverted</p>
      <ProgressBar inverted :progress="testProgress" />
    </div>
  </div>

  <p class="font-bold uppercase mb-4 text-white/80">Message</p>
  
  <div class="flex gap-4 flex-col mb-8">
    <Message>Default</Message>
    <Message type="success">Success</Message>
    <Message type="info">Info</Message>
    <Message type="warning">Warning</Message>
    <Message type="danger">Danger</Message>

    <Message :icon="faSearch">Default - Icon'd</Message>
    <Message type="success" :icon="faSearch">Success - Icon'd</Message>
    <Message type="info" :icon="faSearch">Info - Icon'd</Message>
    <Message type="warning" :icon="faSearch">Warning - Icon'd</Message>
    <Message type="danger" :icon="faSearch">Danger - Icon'd</Message>
    
    <Message header="This is a header">
      This is a message with a header. The header is bold and appears above the message content.
    </Message>

    <Message header="This is a header with icon" :icon="faSearch">
      This is a message with a header and an icon. The header is bold and appears above the message content.
    </Message>
  </div>

  <p class="font-bold uppercase mb-4 text-white/80">Loader</p>
  
  <div class="flex gap-4 flex-col mb-8">
    <div class="flex flex-col gap-2">
      <p>Default</p>
      <Loader />
    </div>
    <div class="flex flex-col gap-2">
      <p>With title</p>
      <Loader title="Look I'm a title!" />
    </div>
    <div class="flex flex-col gap-2">
      <p>With title & subtile</p>
      <Loader title="Look I'm a title!" sub-title="And I'm a subtitle!" />
    </div>
  </div>
</template>