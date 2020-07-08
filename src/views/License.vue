<template>

    <div class="flex flex-1 flex-col lg:p-10 sm:p-5 h-full">
        <div class="flex flex-col md:w-full lg:w-9/12 xl:w-8/12 mx-auto">
            <ftb-button class="py-2 px-4  my-2 w-15" color="primary" css-class="text-center text-l" @click="goTo('/settings')">Back</ftb-button>
            <h1 class="text-2xl">NodeJS Package Licenses</h1>
            <div class="bg-sidebar-item p-5 rounded my-4">
                <div class="flex flex-col my-2">
                    <div class="flex flex-col my-2">
                        <div class="flex flex-row my-4 -mt-2">
                            <div class="relative w-full">
                                <div class="flex items-center bg-blue-500 text-white text-sm font-bold px-4 py-3"
                                     role="alert">
                                    <svg class="fill-current w-4 h-4 mr-2" xmlns="http://www.w3.org/2000/svg"
                                         viewBox="0 0 20 20">
                                        <path d="M12.432 0c1.34 0 2.01.912 2.01 1.957 0 1.305-1.164 2.512-2.679 2.512-1.269 0-2.009-.75-1.974-1.99C9.789 1.436 10.67 0 12.432 0zM8.309 20c-1.058 0-1.833-.652-1.093-3.524l1.214-5.092c.211-.814.246-1.141 0-1.141-.317 0-1.689.562-2.502 1.117l-.528-.88c2.572-2.186 5.531-3.467 6.801-3.467 1.057 0 1.233 1.273.705 3.23l-1.391 5.352c-.246.945-.141 1.271.106 1.271.317 0 1.357-.392 2.379-1.207l.6.814C12.098 19.02 9.365 20 8.309 20z"/>
                                    </svg>
                                    <p>The information on this page was valid as of {{config.dateCompiled | moment}}</p>
                                </div>
                            </div>
                        </div>
                        <div class="flex flex-row my-4 -mt-2" v-for="(license, index) in licenses">
                            <div class="relative">
                                <p>Package Name: {{index}}</p>
                                <p>Repository: <a class="underline cursor-pointer" :href="license.repository"
                                                  target="_blank">{{license.repository}}</a></p>
                                <p>License: {{license.licenses}}</p>
                            </div>
                        </div>
                    </div>
                </div>
            </div>
            <h1 class="text-2xl">Java Dependency Licenses</h1>
            <div class="bg-sidebar-item p-5 rounded my-4">
                <div class="flex flex-col my-2">
                    <div class="flex flex-col my-2">
                        <div class="flex flex-row my-4 -mt-2" v-for="(license, index) in config.javaLicenses">
                            <div class="relative">
                                <p>Package Name: {{index}}</p>
                                <p>Repository: <a class="underline cursor-pointer" :href="license.repository"
                                                  target="_blank">{{license.repository}}</a></p>
                                <p>License: {{license.licenses}}</p>
                            </div>
                        </div>
                    </div>
                </div>
            </div>
        </div>
    </div>
</template>

<script lang="ts">
    import {Component, Vue, Watch} from 'vue-property-decorator';
    import FTBInput from '@/components/FTBInput.vue';
    import FTBToggle from '@/components/FTBToggle.vue';
    import FTBButton from '@/components/FTBButton.vue';
    import FTBSlider from '@/components/FTBSlider.vue';
    import Config from '@/config';
    import Licenses from '../../licenses.json';

    @Component({
        components: {
            'ftb-input': FTBInput,
            'ftb-toggle': FTBToggle,
            'ftb-slider': FTBSlider,
            'ftb-button': FTBButton,
        },
    })


    export default class LicensePage extends Vue {
        private licenses = Licenses;
        private config = Config;

        public goTo(page: string): void {
            // We don't care about this error!
            this.$router.push(page).catch((err) => {
                return;
            });
        }
    }


</script>
