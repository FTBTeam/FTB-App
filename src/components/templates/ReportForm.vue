<template>
  <div class="report-form">
    <div
      class="container flex pt-1 flex-wrap overflow-x-auto justify-center flex-col"
      style="flex-direction: column; justify-content: center; align-items: center"
    >
      <img src="@/assets/images/ftb-logo-full.svg" width="200px" class="mb-8" />
      <div class="wrapper" v-if="!disabled">
        <h1 class="text-2xl text-center">There was an error with the FTB App.</h1>
        <div v-if="!submitted" class="flex flex-col">
          <ftb-input label="Email" v-model="errorEmail" />
          <p>Please describe what happened in the box below and submit.</p>
          <textarea class="bg-navbar border-background-darken p-2" v-model="errorDescription"></textarea>
          <ftb-button color="danger" class="my-2 py-2 px-4 text-center rounded-br" @click="submitError">{{
            submittingError ? 'Submitting...' : 'Submit'
          }}</ftb-button>
        </div>
        <div v-else>
          <p>Thanks for submitting the bug report!</p>
          <ftb-button color="danger" class="my-2 py-2 px-4 text-center rounded-br" @click="quitApp">Quit</ftb-button>
        </div>
      </div>
      <div class="wrapper text-center" v-else>
        <h1 class="text-2xl font-bold">Oh... This doesn't look right...</h1>
        <p class="mt-4 w-1/2 mx-auto">
          It looks like something has gone wrong with the App. It's likely that this is a one off issue, but if it keeps
          happening please report the issue below. We'd recommend trying to restart and see if that fixes it.
        </p>

        <div class="buttons flex justify-center mt-6">
          <a href="https://go.ftb.team/support" target="_blank" class="mr-4">
            <ftb-button color="primary" class="my-2 py-2 text-center rounded-br">Report issue</ftb-button>
          </a>
          <ftb-button color="danger" class="my-2 py-2 text-center rounded-br" @click="quitApp">Quit</ftb-button>
        </div>
      </div>
    </div>

    <div class="debug-info text-left">
      <div class="flex items-start opacity-25">
        <div class="item mr-6">
          <p class="font-bold mb-2">App Version</p>
          <div class="rounded bg-black px-2 py-1 inline-block">{{ publicVersion }}</div>
        </div>
        <div class="item mr-6">
          <p class="font-bold mb-2">UI Version</p>
          <div class="rounded bg-black px-2 py-1 inline-block">{{ uiVersion }}</div>
        </div>
        <div class="item mr-6">
          <p class="font-bold mb-2">Subprocess Version</p>
          <div class="rounded bg-black px-2 py-1 inline-block">{{ appVersion }}</div>
        </div>
        <div class="item">
          <p class="font-bold mb-2">Tries</p>
          <div class="rounded bg-black px-2 py-1 inline-block">Reconnect tries: {{ websockets.reconnects }}</div>
        </div>
      </div>
    </div>
  </div>
</template>

<script lang="ts">
import { Component, Prop, Vue } from 'vue-property-decorator';
import platform from '@/utils/interface/electron-overwolf';
import { Action } from 'vuex-class';

@Component
export default class ReportForm extends Vue {
  @Action('sendMessage') public sendMessage: any;

  @Prop() loadingFailed!: boolean;
  @Prop() websocketsFailed!: boolean;

  @Prop() websockets!: any;
  @Prop() maxTries!: number;

  // TODO: enable once the report form is working again
  disabled = true;

  static emailRegex =
    /^(([^<>()\[\]\\.,;:\s@"]+(\.[^<>()\[\]\\.,;:\s@"]+)*)|(".+"))@((\[[0-9]{1,3}\.[0-9]{1,3}\.[0-9]{1,3}\.[0-9]{1,3}])|(([a-zA-Z\-0-9]+\.)+[a-zA-Z]{2,}))$/;

  uiVersion: string = platform.get.config.webVersion;
  appVersion: string = platform.get.config.appVersion;
  publicVersion: string = platform.get.config.publicVersion;

  errorEmail = '';
  errorDescription = '';
  submittingError = false;
  submitted = false;

  public async submitError() {
    platform.get.actions.uploadClientLogs();
    if (!ReportForm.emailRegex.test(this.errorEmail)) {
      console.log('Email regex not passing');
      return;
    }
    if (this.errorDescription.length === 0) {
      return;
    }
    this.submittingError = true;
    const logLink = await this.uploadLogData().catch((err) => {
      if (err) {
        this.submittingError = false;
        // Show an error here...
        return;
      }
    });
    // TODO: Replace this with an actual endpoint :D
    // Send request
    // await axios.put(`${process.env.VUE_APP_FTB_API}report/create`, {
    //   email: this.errorEmail,
    //   desciption: this.errorDescription,
    //   logs: logLink,
    // });
    //fetch(`https://minetogether.io/api/ftbAppError`, {method: 'PUT', body: JSON.stringify({email: this.errorEmail, logs: logLink, description: this.errorDescription})});
    this.submittingError = false;
    this.submitted = true;
  }

  public uploadLogData(): Promise<string> {
    return new Promise((resolve, reject) => {
      this.sendMessage({
        payload: { type: 'uploadLogs', uiVersion: this.uiVersion },
        callback: async (data: any) => {
          if (!data.error) {
            const url = `https://pste.ch/${data.code}`;
            resolve(url);
          } else {
            reject(data.error);
          }
        },
      });
    });
  }

  quitApp() {
    platform.get.frame.quit();
  }
}
</script>

<style lang="scss" scoped>
.debug-info {
  position: absolute;
  bottom: 1rem;
  left: 1rem;
  padding: 1rem;
  z-index: 9999;
}

.buttons {
  > div,
  > a {
    width: 200px;
  }
}
</style>
